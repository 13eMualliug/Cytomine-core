package be.cytomine.test

import org.apache.commons.io.IOUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.AuthCache
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.protocol.ClientContext
import org.apache.http.entity.ContentProducer
import org.apache.http.entity.EntityTemplate
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.protocol.BasicHttpContext

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 11/02/11
 * Time: 8:18
 * To change this template use File | Settings | File Templates.
 */
class HttpClient {

  DefaultHttpClient client
  HttpHost targetHost
  BasicHttpContext localcontext
  URL URL
  HttpResponse response
  int timeout = 2500;//2.5 seconds

  private Log log = LogFactory.getLog(HttpClient.class)

  void connect(String url, String username, String password)
  {
    log.debug("Connection to " + url + " with login="+username + " and pass=" + password)
    URL = new URL(url)
    targetHost = new HttpHost(URL.getHost(),URL.getPort());
    client = new DefaultHttpClient();
    // Create AuthCache instance
    AuthCache authCache = new BasicAuthCache();
    // Generate BASIC scheme object and add it to the local
    // auth cache
    BasicScheme basicAuth = new BasicScheme();
    authCache.put(targetHost, basicAuth);

    // Add AuthCache to the execution context
    localcontext = new BasicHttpContext();
    localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    // Set credentials
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
    client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
  }

  void get()
  {
    log.debug("Get " + URL.toString())
    HttpGet httpGet = new HttpGet(URL.toString());
    response = client.execute(targetHost, httpGet, localcontext);
  }

    byte[] getData() throws MalformedURLException, IOException, Exception {
        HttpGet httpGet = new HttpGet(URL.toString());
        httpGet.getParams().setParameter("http.socket.timeout", new Integer(timeout));
        response = client.execute(targetHost, httpGet, localcontext);
        log.info("url=" + URL.toString() + " is " + response.getStatusLine().statusCode);

        boolean isOK = (response.getStatusLine().statusCode == HttpURLConnection.HTTP_OK);
        boolean isFound = (response.getStatusLine().statusCode == HttpURLConnection.HTTP_MOVED_TEMP);
        boolean isErrorServer = (response.getStatusLine().statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR);

        if (!isOK && !isFound & !isErrorServer) throw new IOException(URL.toString() + " cannot be read: " + response.getStatusLine().statusCode);
        HttpEntity entity = response.getEntity();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (entity != null) {
            entity.writeTo(baos)
        }
        return baos.toByteArray()
    }

  void delete()
  {
    log.debug("Delete " + URL.toString())
    HttpDelete httpDelete = new HttpDelete(URL.toString());
    response = client.execute(targetHost, httpDelete, localcontext);
  }

  void post(String data)
  {
    log.debug("Post " + URL.toString())
    HttpPost httpPost = new HttpPost(URL.toString());
    log.debug("Post send :" + data.replace("\n",""))
    //write data
    ContentProducer cp = new ContentProducer() {
      public void writeTo(OutputStream outstream) throws IOException {
        Writer writer = new OutputStreamWriter(outstream, "UTF-8");
        writer.write(data);
        writer.flush();
      }
    };
    HttpEntity entity = new EntityTemplate(cp);
    httpPost.setEntity(entity);

    response = client.execute(targetHost, httpPost, localcontext);
  }

  void put(String data)
  {
    log.debug("Put " + URL.toString())
    HttpPut httpPut = new HttpPut(URL.toString());
    log.debug("Put send :" + data.replace("\n",""))
    //write data
    ContentProducer cp = new ContentProducer() {
      public void writeTo(OutputStream outstream) throws IOException {
        Writer writer = new OutputStreamWriter(outstream, "UTF-8");
        writer.write(data);
        writer.flush();
      }
    };
    HttpEntity entity = new EntityTemplate(cp);
    httpPut.setEntity(entity);

    response = client.execute(targetHost, httpPut, localcontext);
  }

    void put(byte[] data) {
        log.debug("Put " + URL.getPath())
        HttpPut httpPut = new HttpPut(URL.getPath());;
        log.debug("Put send :" + data.length)

        InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(data), -1);
        reqEntity.setContentType("binary/octet-stream");
        reqEntity.setChunked(false);
        httpPut.setEntity(reqEntity);
        response = client.execute(targetHost, httpPut, localcontext);
    }






  String getResponseData()   {
    HttpEntity entityResponse = response.getEntity();
    String content = IOUtils.toString(entityResponse.getContent());
    log.debug("Response :" + content.replace("\n",""))
    content
  }

  int getResponseCode() {
    log.debug("Code :" + response.getStatusLine().getStatusCode())
    return response.getStatusLine().getStatusCode()
  }

  void disconnect()
  {
    log.debug("Disconnect")
    try {client.getConnectionManager().shutdown();} catch(Exception e){log.error(e)}
  }
}