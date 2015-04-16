package com.github.rmannibucau.cookit.impl.container;

import com.github.rmannibucau.cookit.impl.configuration.RawConfiguration;
import com.github.rmannibucau.cookit.impl.thread.ThreadSafeWrapper;
import com.github.rmannibucau.cookit.spi.Container;
import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.OWBInjector;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.SingletonService;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

public class OWBContainer implements Container {
    private static final ThreadSafeWrapper<WebBeansContext> CURRENT_CONTAINER = new ThreadSafeWrapper<>();
    private static final boolean DEBUG = Boolean.getBoolean("cookit.spi.debug");

    static {
        if (!DEBUG) { // we don't want spi logs but our stdout friendly ones
            System.setProperty("openwebbeans.logging.factory", OWBNoLog.class.getName());
        }
        WebBeansFinder.setSingletonService(new SingletonService<WebBeansContext>() { // can be called a single time
            @Override
            public WebBeansContext get(final Object o) {
                return CURRENT_CONTAINER.get();
            }

            @Override
            public void clear(final Object o) {
                // no-op
            }
        });
    }

    private WebBeansContext webBeansContext;
    private final Collection<CreationalContext<?>> creationalContexts = new LinkedList<>();

    @Override
    public Container start() { // we'll not tolerate serialization...but why would we need it?
        webBeansContext = new WebBeansContext();
        CURRENT_CONTAINER.set(webBeansContext);
        webBeansContext.getService(ContainerLifecycle.class).startApplication(null);
        return this;
    }

    @Override
    public boolean isStarted() {
        return webBeansContext != null;
    }

    @Override
    public <T> T inject(final T instance) {
        try {
            final BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
            final CreationalContextImpl<?> creationalContext = beanManager.createCreationalContext(null);
            creationalContexts.add(creationalContext);
            OWBInjector.inject(beanManager, instance, creationalContext);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    private <T> T internalInstance(final Class<T> type) { // for internal normal scoped beans only
        final BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
        return type.cast(bm.getReference(bm.resolve(bm.getBeans(type)), type, null));
    }

    @Override
    public Map<String, Object> configuration() {
        return internalInstance(RawConfiguration.class).getMap();
    }

    @Override
    public void fire(final Object event) {
        webBeansContext.getBeanManagerImpl().fireEvent(event);
    }

    @Override
    public void close() {
        if (webBeansContext != null) {
            creationalContexts.stream().forEach(CreationalContext::release);
            webBeansContext.getService(ContainerLifecycle.class).stopApplication(null);
            CURRENT_CONTAINER.reset();
        }
    }

    @Override
    public Object[] createParameters(final Object lambda) {
        final BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();

        Type[] paramTypes = null;
        Annotation[][] paramAnnotations = null;
        try {
            final Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            final SerializedLambda serializedLambda = SerializedLambda.class.cast(method.invoke(lambda));
            final String signature = serializedLambda.getImplMethodSignature();
            final MethodType mt = MethodType.fromMethodDescriptorString(signature, lambda.getClass().getClassLoader());
            paramTypes = mt.parameterArray();
            paramAnnotations = new Annotation[paramTypes.length][0];
        } catch (final Exception e) {
            // not a lambda try pure reflection, algo is trivial since it is functional interfaces: take the only not Object methods ;)
            for (final Method m : lambda.getClass().getMethods()) {
                if (Object.class == m.getDeclaringClass()) {
                    continue;
                }

                paramTypes = m.getParameterTypes();
                paramAnnotations = m.getParameterAnnotations();
                break;
            }
        }
        if (paramTypes == null || paramAnnotations == null) {
            throw new IllegalStateException("Can't find parameters for " + lambda);
        }

        final Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < args.length; i++) {
            final CreationalContext<?> creational = manager.createCreationalContext(null);
            creationalContexts.add(creational);
            args[i] = manager.getInjectableReference(new MethodParamInjectionPoint(paramTypes[i], paramAnnotations[i], i, manager), creational);
        }
        return args;
    }

    private static final class MethodParamInjectionPoint implements InjectionPoint {
        private final int position;
        private final Set<Annotation> qualifiers = new HashSet<>();
        private final Type baseType;

        private MethodParamInjectionPoint(final Type baseType,
                                          final Annotation[] annotations,
                                          final int position, final BeanManager beanManager) {
            this.baseType = baseType;
            this.position = position;

            if (annotations != null) {
                for (final Annotation annotation : annotations) {
                    if (beanManager.isQualifier(annotation.annotationType())) {
                        qualifiers.add(annotation);
                    }
                }
            }
            if (qualifiers.isEmpty()) {
                qualifiers.add(DefaultLiteral.INSTANCE);
            }
            qualifiers.add(AnyLiteral.INSTANCE);
        }

        @Override
        public Type getType() {
            return baseType;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            return null;
        }

        @Override
        public Annotated getAnnotated() {
            return new ParamAnnotated(qualifiers, baseType, position);
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }

    private static final class ParamAnnotated implements AnnotatedParameter<Object> {
        private final int position;
        private final Set<Type> types = new HashSet<>();
        private final Set<Annotation> annotations;
        private final Type baseType;

        private ParamAnnotated(final Set<Annotation> annotations, final Type baseType, final int position) {
            this.baseType = baseType;
            this.position = position;

            this.types.add(getBaseType());
            this.types.add(Object.class);

            this.annotations = new HashSet<>(annotations);
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Override
        public AnnotatedCallable<Object> getDeclaringCallable() {
            return null;
        }

        @Override
        public Type getBaseType() {
            return baseType;
        }

        @Override
        public Set<Type> getTypeClosure() {
            return types;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
            for (final Annotation a : annotations) {
                if (a.annotationType().getName().equals(annotationType.getName())) {
                    return (T) a;
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }
}
