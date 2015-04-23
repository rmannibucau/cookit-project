package com.github.rmannibucau.cookit.maven.resolver;

import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.dependency.Maven;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static java.util.Arrays.asList;

@ApplicationScoped
public class MavenResolver implements Maven {
    private DefaultRepositorySystemSession session;
    private RepositorySystem repositorySystem;

    @Inject
    @Value(key = "cookit.maven.localRepository", or = "${user.home}/.cookit/m2/repository/")
    private String localRepositoryPath;

    @Inject
    @Value(key = "cookit.maven.remoteRepositories")
    private String remoteRepositories;

    @Inject
    @Value(key = "cookit.maven.localOnly", or = "false")
    private Boolean localOnly;

    @Inject
    @Value
    private Properties configuration;

    @PostConstruct
    private void buildWorld() {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(final Class<?> type, final Class<?> impl, final Throwable exception) {
                throw new IllegalStateException(exception);
            }
        });

        repositorySystem = locator.getService(RepositorySystem.class);

        changeRepository(localRepositoryPath);
    }

    public String getLocalRepositoryPath() {
        return localRepositoryPath;
    }

    public void changeRepository(final String path) {
        localRepositoryPath = path;

        session = new DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, new LocalRepository(path)));
    }

    @Override
    public File resolve(final String coords, final String repository) {
        final ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(new DefaultArtifact(coords));

        if (repository != null) {
            final RemoteRepository repo = new RemoteRepository.Builder("custom", "default", "central".equals(repository) ? "http://repo1.maven.org/maven2/" : repository).build();
            if (!localOnly || "file".equals(repo.getProtocol())) {
                artifactRequest.addRepository(repo);
            }
        } else {
            if (remoteRepositories != null) {
                asList(remoteRepositories.split(" *, *")).stream().forEach(r -> {
                    String url = "central".equals(r) ? "http://repo1.maven.org/maven2/" : configuration.getProperty(r + ".url");
                    if (url != null && (!localOnly || url.startsWith("file:"))) {
                        artifactRequest.addRepository(new RemoteRepository.Builder(r, configuration.getProperty(r + ".type", "default"), url).build());
                    }
                });
            } else if (!localOnly) {
                artifactRequest.addRepository(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());
            }
        }

        try {
            return repositorySystem.resolveArtifact(session, artifactRequest).getArtifact().getFile();
        } catch (final ArtifactResolutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
