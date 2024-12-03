## 污点打在request

#### 传播路径

```
Server: main(String[] args) req ->
ApplicationFilterChain: doFilter(ServletRequest request, ServletResponse response) request ->
ApplicationFilterChain: internalDoFilter(ServletRequest request, ServletResponse response) request ->
HttpServlet: service(ServletRequest req, ServletResponse res) req ->
HttpServlet: service(ServletRequest req, ServletResponse res) request ->
DefaultServlet: service(HttpServletRequest req, HttpServletResponse resp) req ->
DefaultServlet: doGet(HttpServletRequest request, HttpServletResponse response) request ->
DefaultServlet: serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String inputEncoding) request --transfer-->
DefaultServlet: serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String inputEncoding) path --transfer-->
DefaultServlet: serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String inputEncoding) resource --transfer-->
DefaultServlet: serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String inputEncoding) resourceBody ->
OutputStream: write(byte b[]) b
```

#### 需要的transfer

```yml
{ method: "<org.apache.catalina.webresources.CachedResource: byte[] getContent()>", from: base, to: result, type: "byte[]" }
{ method: "<org.apache.catalina.servlets.DefaultServlet: java.lang.String getRelativePath(javax.servlet.http.HttpServletRequest,boolean)>", from: 0, to: result, type: java.lang.String }
{ method: "<org.apache.catalina.webresources.StandardRoot: org.apache.catalina.WebResource getResource(java.lang.String)>", from: 0, to: result, type: org.apache.catalina.webresources.CachedResource }
```

## 污点打在request内部存储的String

#### 传播路径

第一段污点传播
```
<Server: void main(java.lang.String[])>/temp$5  进入Request的一个Map类型内部
<org.apache.catalina.servlets.DefaultServlet: java.lang.String getRelativePath(javax.servlet.http.HttpServletRequest,boolean)>/$r3(pathInfo) and $r4(servletPath) --transfer-->
<org.apache.catalina.servlets.DefaultServlet: java.lang.String getRelativePath(javax.servlet.http.HttpServletRequest,boolean)>/$r8(result) --transfer-->
<org.apache.catalina.servlets.DefaultServlet: void serveResource(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse,boolean,java.lang.String)>/$r2(path)
```
第二段污点传播
```
<org.apache.catalina.webresources.CachedResource: byte[] cachedContent>
<org.apache.catalina.webresources.CachedResource: byte[] getContent()>/r3(cachedContent)
<org.apache.catalina.servlets.DefaultServlet: void serveResource(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse,boolean,java.lang.String)>/r113(resourceBody)
```

#### 需要的transfer

```yml
  { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", from: 0, to: base, type: "java.lang.StringBuilder" }
  { method: "<java.lang.StringBuilder: java.lang.String toString()>", from: base, to: result, type: "java.lang.String" }
```
