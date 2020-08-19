// Databricks notebook source
// MAGIC %md
// MAGIC https://docs.databricks.com/dev-tools/api/latest/groups.html#
// MAGIC 
// MAGIC List members
// MAGIC 
// MAGIC Endpoint	HTTP Method
// MAGIC 
// MAGIC 2.0/groups/list-members	GET
// MAGIC 
// MAGIC Return all of the members of a particular group. This call returns an error RESOURCE_DOES_NOT_EXIST if a group with the given name does not exist.

// COMMAND ----------

/*Example request:

JSON
{
  "group_name": "Gryffindor"
}
Example response:

JSON
{
  "members": [
    { "user_name": "hjp@hogwarts.edu" },
    { "user_name": "hermione@hogwarts.edu" },
    { "user_name": "rweasley@hogwarts.edu" },
    { "group_name": "Gryffindor Faculty" }
  ]
}
*/

// COMMAND ----------

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.http.impl.client.{DefaultHttpRequestRetryHandler, HttpClientBuilder, HttpClients}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.methods.HttpGet
import java.sql.Timestamp

// COMMAND ----------

val workspace_host      = dbutils.notebook.getContext.toMap.get("tags").get.asInstanceOf[Map[String,String]]("browserHostName")
val workspace_id        = dbutils.notebook.getContext.toMap.get("tags").get.asInstanceOf[Map[String,String]]("orgId")
val dbws_token          = dbutils.secrets.get("db-keyvault-scope", "workspace-token")

// COMMAND ----------

// DBTITLE 1,Get Group list
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
val gp_list=df_gp_reponse.select(explode($"group_names").alias("group_name")).map(_.getString(0)).collect().toList


// COMMAND ----------

// DBTITLE 1,Get Users list
var df_gp_users:DataFrame=null
val processDate=Timestamp.valueOf(java.time.LocalDateTime.now)

for(gp_name <- gp_list)
{
  val df_gp_reponse=getHttpResponse("https://"+workspace_host+"/api/2.0/groups/list-members","group_name",gp_name)
  val df_users=df_gp_reponse.select(explode($"members").alias("members")).select($"members.user_name")
  .withColumn("group_name",lit(gp_name))
  .withColumn("date",to_date(lit(processDate),"yyyy-MM-dd"))
   if(df_gp_users==null)
   {
     df_gp_users=df_users
   }else{
     df_gp_users=df_gp_users.union(df_users)
   }
}

// COMMAND ----------

// DBTITLE 1,Access Group Api using python
// MAGIC %python
// MAGIC import http.client
// MAGIC import mimetypes
// MAGIC 
// MAGIC conn = http.client.HTTPSConnection("adb-<********>.5.azuredatabricks.net")
// MAGIC payload = "{\r\n    \"group_name\": \"admins\"\r\n}"
// MAGIC headers = {
// MAGIC   'Content-Type': 'application/json',
// MAGIC   'Authorization': 'Bearer <your databricks token>'
// MAGIC }
// MAGIC conn.request("GET", "/api/2.0/groups/list-members", payload, headers)
// MAGIC res = conn.getresponse()
// MAGIC data = res.read()
// MAGIC print(data.decode("utf-8"))

// COMMAND ----------

// DBTITLE 1,Access Group Api using curl
// MAGIC %sh
// MAGIC curl --location --request GET 'https://adb-<*******>.5.azuredatabricks.net/api/2.0/groups/list-members' \
// MAGIC --header 'Content-Type: application/json' \
// MAGIC --header 'Authorization: Bearer <your databricks token>' \
// MAGIC --data-raw '{
// MAGIC     "group_name": "admins"
// MAGIC }'
