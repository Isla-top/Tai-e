## CVE-2020-1938

### 漏洞名称：Apache Tomcat远程文件包含漏洞

### 漏洞描述

### sink方法
```yml
{ method: "<org.apache.catalina.connector.CoyoteOutputStream: void write(byte[])>", index: 0 }
```

### source方法
```yml
{ kind: param, method: "<org.apache.catalina.core.ApplicationFilterChain: void doFilter(javax.servlet.ServletRequest,javax.servlet.ServletResponse)>", index: 0, type: "org.apache.catalina.connector.Request" }
```

### 需推测的transfer
```yml
{ method: "<org.apache.catalina.webresources.CachedResource: byte[] getContent()>", from: base, to: result, type: "byte[]" }
{ method: "<org.apache.catalina.servlets.DefaultServlet: java.lang.String getRelativePath(javax.servlet.http.HttpServletRequest,boolean)>", from: 0, to: result, type: java.lang.String }
{ method: "<org.apache.catalina.webresources.StandardRoot: org.apache.catalina.WebResource getResource(java.lang.String)>", from: 0, to: result, type: org.apache.catalina.webresources.CachedResource }
```

### 评估结果

### 污点路径

每一行:

(class name): (method signature) (taint variable name)

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

