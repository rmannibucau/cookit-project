<%include "header.gsp"%>
	
	<%include "menu.gsp"%>
	
	<div class="page-header">
		<div class="container">
			<div class="row">
				<div class="col-sm-12">
					<h1>${content.title}</h1>
				</div>
			</div>
		</div>
	</div>

	<div class="container">
	<p><em>${content.date.format("dd MMMM yyyy")}</em></p>

	<p>${content.body}</p>

	<hr />
	</div>
	
<%include "footer.gsp"%>