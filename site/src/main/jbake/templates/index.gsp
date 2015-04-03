<%include "header.gsp"%>
	<%include "menu.gsp"%>

	<% if (content.body) { %>
		${content.body}
	<% } else if (new File('.').canonicalFile.name == 'site') { %>
		${new File('./src/main/jbake/templates/index.html').text}
	<% } else { %>
		${new File('./site/src/main/jbake/templates/index.html').text}
	<% } %>
<%include "footer.gsp"%>