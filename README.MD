# Databricks Group Api Access

## How to get Groups/Users list using Databricks Rest Api.

The Groups API allows you to manage groups of users. This code will allow to access databricks group api from notebook. I written this code snipped for one of my requirement, where i have to send workspace security events to security team.  
This is one of example to access Databricks Api but with this code you can access any databricks Api. like Clusters API



[DBFS API](https://docs.databricks.com/dev-tools/api/latest/dbfs.html)

[Workspace API](https://docs.databricks.com/dev-tools/api/latest/workspace.html)

[Jobs API](https://docs.databricks.com/dev-tools/api/latest/jobs.html)

[Groups API](https://docs.databricks.com/dev-tools/api/latest/groups.html)

[Libraries API](https://docs.databricks.com/dev-tools/api/latest/libraries.html)

[Secrets API](https://docs.databricks.com/dev-tools/api/latest/secrets.html)

[SCIM API](https://docs.databricks.com/dev-tools/api/latest/scim/index.html)

[Permissions API](https://docs.databricks.com/dev-tools/api/latest/permissions.html)

[Global Init Scripts API](https://docs.databricks.com/dev-tools/api/latest/global-init-scripts.html)

[Cluster Policies APIs](https://docs.databricks.com/dev-tools/api/latest/clusters.html)

[Token API](https://docs.databricks.com/dev-tools/api/latest/tokens.html)

[Token Management API](https://docs.databricks.com/dev-tools/api/latest/token-management.html)

### scala
```scala
def getHttpResponse(url:String,param_key:String,param_value:String):DataFrame={
  val client = HttpClientBuilder.create()
  .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
  .build()

  val builder = new URIBuilder(url)
  if(param_key!="")
  {
    builder.setParameter(param_key, param_value)
  }
  val httpGet = new HttpGet(builder.build());
  httpGet.setHeader("Accept", "application/json")
  httpGet.setHeader("Content-type", "application/json")
  httpGet.setHeader("Authorization", "Bearer "+dbws_token)
  val response = client.execute(httpGet)
  val inputStream = response.getEntity.getContent
  val content = scala.io.Source.fromInputStream(inputStream).mkString
  val df_gp_list = spark.read.json(Seq(content).toDS)
  df_gp_list
}

val df_gp_reponse=getHttpResponse("https://"+workspace_host+"/api/2.0/groups/list","","")
val df_gp_reponse=getHttpResponse("https://"+workspace_host+"/api/2.0/groups/list-members","group_name",gp_name)
```
### python
```python
import http.client
import mimetypes

conn = http.client.HTTPSConnection("adb-<********>.5.azuredatabricks.net")
payload = "{\r\n    \"group_name\": \"admins\"\r\n}"
headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <your databricks token>'
}
conn.request("GET", "/api/2.0/groups/list-members", payload, headers)
res = conn.getresponse()
data = res.read()
print(data.decode("utf-8"))
```
### curl
```curl
curl --location --request GET 'https://adb-<*******>.5.azuredatabricks.net/api/2.0/groups/list-members' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <your databricks token>' \
--data-raw '{
    "group_name": "admins"
}'
```
