<%include "header.gsp"%>

	<%include "menu.gsp"%>

	<div class="page-header">
    		<div class="container">
    			<div class="row">
    				<div class="col-sm-12">
    					<h2>Blog</h2>
    				</div>
    			</div>
    		</div>
    	</div>
	<div class="container">
	<%published_posts.each {post ->%>
		<a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>${post.uri}"><h1>${post.title}</h1></a>
		<p><small>${post.date.format("dd MMMM yyyy")}</small></p>
		<p>${post.body}</p>
  	<%}%>
	
	<hr />
	
	<p>Older posts are available in the <a href="/${config.archive_file}">archive</a>.</p>
	</div>

<%include "footer.gsp"%>