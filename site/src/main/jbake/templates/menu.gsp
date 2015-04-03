	<!-- Fixed navbar -->
    <header class="navbar navbar-default navbar-static-top" id="top" role="banner">
      <a href="https://github.com/rmannibucau/cookit">
        <img style="position: absolute; top: 0; right: 0; border: 0;"
             src="img/github-fork.png" alt="Fork me on GitHub"
             data-canonical-src="img/github-fork.png">
      </a>
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %>/<% }%>">CooKit</a>
        </div>
        <div class="navbar-collapse collapse">
          <ul class="nav navbar-nav navbar-right">
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>download.html">Download</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>documentation.html">Documentation</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>blog/index.html">Blog</a></li>
          </ul>
        </div><!--/.nav-collapse -->
      </div>
    </header>