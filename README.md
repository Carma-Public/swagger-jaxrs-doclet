# Swagger Doclet 

[![Build Status](https://travis-ci.org/teamcarma/swagger-jaxrs-doclet.svg?branch=master)](https://travis-ci.org/teamcarma/swagger-jaxrs-doclet)

This is a JavaDoc Doclet that can be used to generate a JSON Swagger resource listing suitable for feeding to the Swagger UI. It generates the resource listing from the source code of java JAXRS resources and entities.

This was originally forked from [Ryan Kennedy's original doclet](https://github.com/ryankennedy/swagger-jaxrs-doclet) to add support for swagger 1.2 and has moved on considerably from it.

This is used as a basis for the [Carma API Reference](https://api-dev.car.ma/apidoc/ref/index.html) which is a good working example of the Swagger UI being used for a large API.

#####Note you can win [$1,000,000](http://carmacarpool.com/prize) using the Carma API! 


##Why Use This Doclet?

+ As the JSON resource listing  is generated offline from source code it means that you do not need to add any runtime dependencies to your project and avoid potential headaches with different jar versions and jaxrs implementations. This also avoids increasing the size of your artifacts. 
+ The offline approach also allows you to post process the JSON if you wish. 
+ You also do not need to add any Swagger specific annotations to your source code as you can fine tune the generated documentation using javadoc tags.

## Versions

1.1.x Versions and higher require Java 8

1.0.x Versions require Java 6/7

The latest production version is 1.1.1.

This contains the following fixes/features:

+ Fix incorrect detection of sub resource methods (also improves performance) (Issue 107)
+ Support new Java 8 Date Time types (Issue 104)
+ Fix readme in relation to resourceRootPath (Issue 102)
+ Group multiple response messages with same http status together for compatibility with swagger ui/spec (Issue 97)
+ Fix performance degradation (Issue 94)

The fixes in the 1.1.0 version were as follows:

+ Support Java 8 (Issue 83)
+ Upgrade swagger UI to 2.1.0; fixes: 
	+ Api ordering in the embedded UI (Issue 70) 
	+ Success code not being displayed (Issue 77)
+ Adding support for arrays of enums -courtesy of @tandrup (Issue 93) 


The fixes in the 1.0.5 version were as follows:

+ Added better support for subTypes - courtesy of @mhardorf (Issue 86)
+ Class PathParam Variables Not Being Added as Required Parameters to JSON Output - big help from @nkoterba (Issue 74)
+ Add support for data type format (Issue 84)
+ Support not including private model fields by default via -defaultModelFieldsXmlAccessType flag (Issue 85) 
+ Support BigDecimal and BigInteger (Issue 87)
+ Support allowable values javadoc tag (Issue 89)
+ Update Documentation for Gradle Users in Resolving Models - courtesy of @nkoterba (Issue 82)
+ Type identification does not work properly when mixing array types / regular types (Issue 81)
+ Provide includeResourcePrefixes Configuration Option (Issue 80)
+ @responseType doesn't support primitives (Issue 76)
+ Issue Creating Paths with Regex Expressions - big help from @nkoterba (Issue 73)
+ Model generation does not work properly for collections in some cases (Issue 72)
+ Support for array types (Issue 71)
+ @responseType ignored where method signature specifies generic return type (Issue 69)

1.0.4.2 is a patch release of the 1.0.4 version to fix two additional bugs in the 1.0.4 release:

+ Classes referenced by a Collection but that do not occur as parameters are not included in the model (Issue 58)
+ @Size on method parameter causes the model parsing to fail (Issue 57)

1.0.4.1 is a patch release of the 1.0.4 version to fix a bug with @Size validation annotation on non numeric fields (Issue 53)


The fixes/features in the 1.0.4 version were as follows:

+ Add support for javax.validation annotations (JSR-303) on DTOs (Issue 51)
+ Doclet fails when destination directory does not exist (Issue 50)
+ Support of HttpMethods PATCH/OPTIONS/HEAD (Issue 49)
+ support wrapped types for paramaters e.g. Option < String > (Issue 47)
+ support ordering response codes numerically (Issue 42)
+ Workaround for Plugin fails when ${basedir} contains a space for apiInfoFile (Issue 41)
+ abstract sub resources dont work (Issue 46)
+ Doclet enters infinite loop when generic class parameter cannot be concretized (Issue 45)
+ Resource with @Path("/") is ignored (Issue 44)
+ NPE from missing model id when encountering @XmlTransient class (Issue 39)
+ support generic JAXRS sub-resources (Issue 38)
+ @Produces on class does not get propagated to method-level swagger docs (Issue 36)

The fixes/features in the 1.0.3 version were as follows:

+ json view with nested jsonview annotated fields can cause duplicate model exception (Issue 35)
+ Shipped swagger zip has nested directory (Issue 34)
+ Ignore getters with parameter (Issue 33)
+ Unable to set param as required when wrapped in @BeanParam (Issue 32)
+ Support gradle (Issue 27)
+ Support configuring json serialization/deserialization (Issue 26)

The fixes/features in the 1.0.2 version were as follows:

+ Request body pojos not working with XmlAccessType.FIELD (Issue 25)
+ @QueryParams are rendered as BODY type params when inside of a @BeanParam (Issue 31)
+ Joda time classes are not handled meaning deep models are generated for them that the UI can't handle (Issue 24)
+ Getters that return a different type to their field are not supported (portion of Issue 17)
+ Getters/Setters without a corresponding field and which use a custom name are not supported (portion of Issue 17)
+ Boolean getters that use the is* naming convention can result in duplicate fields (portion of Issue 17)
+ Add option to disable use of @XmlAccessorType (portion of Issue 25)
+ For @XmlAccessorType Support Public Member, None and allow jaxb annotated fields/members to override the default Field and Property behaviour (portion of Issue 25)
+ Support Json Subtypes (Issue 22)
+ Support project wide naming conventions like lowercase underscore separated for model fields (portion of Issue 17) 

The fixes/features in the 1.0.1 version were as follows:

+ custom response type not being added to model (issue 21)
+ support api level descriptions (issue 19)
+ @XmlAttribute name is not being used for model field names (issue 18)
+ support variables in the javadoc which we can replace with values from a properties file (issue 14)
+ Support relative basePath w/ port (issue 20)
+ @XmlTransient or @JsonIgnore on setters can lead to invalid model fields (portion of issue 17)

The latest snapshot version is 1.1.2-SNAPSHOT.

## Usage

### Maven

To use the Swagger Doclet in your Maven project, add the following to your POM file.

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>…</groupId>
    <artifactId>…</artifactId>
    <version>…</version>
    
    <dependencies>
        …
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>2.9.1</version>
                <executions>
                    <execution>
                        <id>generate-service-docs</id>
                        <phase>generate-resources</phase>
                        <configuration>
                            <doclet>com.carma.swagger.doclet.ServiceDoclet</doclet>
                            <docletArtifact>
                                <groupId>com.carma</groupId>
								<artifactId>swagger-doclet</artifactId>
        						<version>1.0.5</version>
        					</docletArtifact>
                            <reportOutputDirectory>${project.build.outputDirectory}</reportOutputDirectory>
                            <useStandardDocletOptions>false</useStandardDocletOptions>
                            <additionalparam>-apiVersion 1 -docBasePath http://myapi -apiBasePath http://myapi</additionalparam>
                        </configuration>
                        <goals>
                            <goal>javadoc</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</xml>

```

### Gradle

Here is an example build.gradle file that will generate the swagger JSON files in the build/reports/rest-api-docs directory:

```

 apply plugin: 'java'

 configurations {
   doclet
 }

 repositories {
    mavenLocal()
    mavenCentral()
 } 

dependencies {
    doclet(
        [group: 'com.carma', name: 'swagger-doclet', version: '1.0.5'],
        [group: 'javax.ws.rs', name: 'javax.ws.rs-api', version: '2.0']
    ) 
}

 task generateRestApiDocs(type: Javadoc) {
   source = sourceSets.main.allJava
   destinationDir = reporting.file("rest-api-docs")
   options.classpath = configurations.doclet.files.asType(List)
   options.docletpath = configurations.doclet.files.asType(List)
   options.doclet = "com.carma.swagger.doclet.ServiceDoclet"
   options.addStringOption("apiVersion", "1")
   options.addStringOption("docBasePath", "/sps/apidocs")
   options.addStringOption("apiBasePath", "/sps")
   options.addBooleanOption("skipUiFiles", true)
}
```

#### Including Model Classes from outside the doclet project
Thanks to @nkoterba for these gradle instructions described in issue 82.
There are two solutions:

**Solution 1**
For those **not** using Intelli-J, you can just define a sourceset that points to the source of your current project where the Doclet plugin is run + any Model projects:

```
sourceSets {
    doclet {
        java {
            srcDir 'src/main/java'
            // List all the source project directories where we define
            // Models that will be used by Doclet to generate REST API Docs
            srcDir project(":code:models:api").file("src/main/java")
        }
    }
}
```

Then update your task that generates the REST API Docs from:
```
source = sourceSets.main.allJava
```

to:
```
source = sourceSets.doclet.allJava
```

**Solution 2**
Unfortunately, if you are using Intelli-J, it's Gradle Sync/Plugin will complain if you define a SourceSet with sources outside of your project. See: https://youtrack.jetbrains.com/issue/IDEA-122577#tab=Comments

So instead, you have to "manually" build your source set:

```
task generateRestApiDocs(type: Javadoc) {
    // Ideally would use a custom-defined source set above
    // However, Intelli-J can't handle Sources out of content root
    // See: https://youtrack.jetbrains.com/issue/IDEA-122577#tab=Comments
    // and: https://github.com/spockframework/spock/issues/70
    // So instead "manually" creating the source set

    def sourceList = new ArrayList<File>()

    sourceList.addAll(sourceSets.main.allJava);
    project(":code:models:api").fileTree("src/main/java").each {
        sourceList.add(it)
    }

    source = sourceList
    options.classpath = configurations.doclet.files.asType(List)
    options.docletpath = configurations.doclet.files.asType(List)

   // ... Rest of doclet setup/configuration here
   
```

### Usage Notes
The settings that you use for the doc base path and the api base path will vary depending on both the version of swagger that is used and the name used for your top level resource listing. Please refer to the Doclet Options section of this document for a detailed description of these. 

The doclet will generate a service.json file which is the swagger spec 1.2 resource listing and will also generate a series of resource json files. To use the swagger ui with the generated json files you can either use the swagger ui that is embedded with this doclet (currently 2.1.8-M1) or use your own which is recommended for most people as it allows you to tweak the look and feel.


You would then add various javadoc tags to your source code to fine tune the generated documentation.
The generated documentation will be written as a series of JSON files in a format that the Swagger UI can read. You would typically deploy the Swagger UI and your API JSON files on a webserver.

To see a working example of the generated documentation refer to the [example](#example) below.

NOTE: If you use your own copy of the swagger ui then you will need to modify the index.html so that the url points not to the swagger petstore example but to the service.json file generated by this doclet.
For example if the service.json is in a dir called apidocs then the change to index.html would look like this:

```
window.swaggerUi = new SwaggerUi({
      url: "apidocs/service.json"
```

Note: If you are using a snapshot version then these are deployed in the sonatype snapshots repository so you will either need to checkout the snapshot version and run mvn install on it or you will need to include the Sonatype Nexus Snapshots repository. For Maven you can put the following in either in your settings.xml or pom.xml.

```
<repositories>
...
		<repository>
			<id>sonatype-nexus-snapshots</id>
			<name>Sonatype Nexus Snapshots</name>
			<url>http://oss.sonatype.org/content/repositories/snapshots</url>
			<releases>
				<enabled>false</enabled>
			</releases>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
		</repository>
		...
	</repositories>

```

## Supported Javadoc Tags and Annotations

<table>
	<tr><th>Tag</th><th>Purpose</th><th>Applies To</th><th>Aliases</th></tr>
	<tr><td>@exclude</td><td>Use to exclude a resource class, operation, model field or method from the generated documentation</td><td>classes, operations, model methods & fields</td><td>@hidden, @hide</td></tr>
	<tr><td>@excludeParams</td><td>Use to exclude operation parameters from the generated documentation</td><td>operations</td><td>@hiddenParams, @hideParams</td></tr>
	<tr><td>@responseMessage</td><td>Use to describe each success or response status returned from an operation. It is made up of a numeric status code, a description an an optional response model specifically for this error.<br><br>For example:<br><br>
	@responseMessage 404 not found<br><br>
	@responseMessage 404 not found `fixtures.responsemessages.Response1<br><br>
	NOTE: The optional response model class is added after a backtick as in the example above.
	NOTE 2: By default the response messages are ordered in ascending order of response code so success codes come before error codes. You can change this using the responseMessageSortMode doclet option described below.
	</td><td>operations</td><td>@status, @errorResponse, @successResponse, @errorCode, @successCode</td></tr>
	<tr><td>@responseType</td><td><p>If you want the documented response model class to be different to the one in the java method signature you can use this to override it. This is useful to replace the Jax Rs response class with a particular entity type that may be returned. For example:</p>
	<p>
	<code><pre>
	/**
	 * @responseType fixtures.responsemodel.Response2
	 */
	@SuppressWarnings("javadoc")
	@GET 
	public Response responseDefinedViaTag() {
		return Response.ok().entity(new Response2()).build();
	}
	</pre></code></p><p>In the above method the response model documented will be fixtures.responsemodel.Response2.</p>
	<p><i>NOTE: if the response class is not in the same maven project as the one the doclet plugin is on then you will need to include the response class in the source of your project. One way to do this in maven is to use the maven-dependency-plugin's unpack goal to unpack the *.java files of the dependency source jar to a temporary directory such as ${project.build.directory}/additional-sources and then attach that directory to your project source using the build-helper-maven-plugin's add-source goal.</i></p>
	<p>You can also use collections both of entities:</p>
	<p><code><pre>
	@responseType&nbsp;java.util.List&lt;fixtures.responsemodel.Response2&gt;	</pre>
	</code>
	</p>
	<p>and primitives:</p>
	<p><code><pre>
	@responseType&nbsp;java.util.List&lt;java.lang.String&gt;
	</pre>
	</code>
	</p>

	</td><td>operations</td><td>@outputType, @returnType</td></tr>
	<tr><td>@inputType</td><td>If you want to have a different class type used as the model for a body parameter then you can use this. For example: 
	<p>
	<code><pre>
	/**
	 * @inputType fixtures.inputtype.Data2
	 */
	@POST
	@Path("/postData2b")
	public Response postData2b(@HeaderParam("X-forwarded-for") String xFwd, @QueryParam("p1") int p1, Data1 data) {
		return Response.ok().build();
	}
	</pre></code></p>In the above method the body parameter model documented will be fixtures.inputtype.Data2
	</td><td>PUT/POST operations</td><td>@bodyType</td></tr>
	<tr><td>@defaultErrorType</td><td>If error responses result in an error entity and you want to document this without having to put in each error responseMessage you can set this either on the operation method or the resource classes javadoc.</td><td>operations,resource classes</td><td></td></tr>
	
	<tr><td>@apiDescription</td><td>This is used for the api description where an api can have multiple operations of different HTTP methods under the same path. As such any of the java methods that are used for the different operations of an api can specify the @apiDescription.</td><td>operations</td><td></td></tr>
	
	<tr><td>@description</td><td>For model properties this is used for the field description. For operations this is used for the notes. For operations if you do not use this then the notes will be taken from the commment text minus the first sentences of the javadoc.</td><td>operations, model methods</td><td>@comment, @return - (only for model methods)</td></tr>
	
	<tr><td>@summary</td><td>This is used for the summary of the operation. If you do not use this then the summary will be taken from the first sentences of the javadoc.</td><td>operations</td><td>@endpointName</td></tr>
	
	<tr><td>@format</td><td>Defines the format for a model field. Note that this is only used for types that do not already map to a predefined format; which primarily means string type.</td><td>model fields and methods</td><td></td></tr>
	
	<tr><td>@min</td><td>Defines a minimum value for a model field.</td><td>model fields and methods</td><td>@minimum</td></tr>
	<tr><td>@max</td><td>Defines a maximum value for a model field.</td><td>model fields and methods</td><td>@maximum</td></tr>
	
	<tr><td>@default</td><td>Defines a default value for a model field. For enums this must be one of the enum values, for numeric types if there is a minimum value then this must be >= to it, similarly if there is a maximum value then this must be <= to it.</td><td>model fields and methods</td><td>@defaultValue</td></tr>
	
	<tr><td>@requiredField</td><td>This sets whether a model field is required.</td><td>model fields and methods</td><td>@required</td></tr>
	
	<tr><td>@optionalField</td><td>This sets whether a model field is optional. This is only needed if the doclet option -modelFieldsRequiredByDefault has been enabled.</td><td>model fields and methods</td><td>@optional</td></tr>
	
	<tr><td>@requiredParams</td><td>Defines a csv of operation parameter names that are required. If a param is not in the requiredParams list then it is required if it is a path or body param unless it is in the @optionalParams list. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td></td></tr>
	
	<tr><td>@optionalParams</td><td>Defines a csv of operation parameter names that are optional. If a param is not in the optionalParams list then it is optional if it is NOT a path or body param unless it is in the @requiredParams list. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td></td></tr>
	
	<tr><td>@csvParams</td><td>Defines a csv of operation parameter names that use CSV values. For swagger 1.2 this results in the allowMultiple field being set to true, however the Swagger UI does not support this at present. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td></td></tr>
	
	<tr><td>@implicitParam</td><td>Defines an extra parameter that should be added to a resource method. A method or class can have 0 to many of these. If placed at the class level then all methods of the resource class will have the parameter added. This could for example be used for a common security header. The format for the tag is as follows: <br>
	<pre>
	name|dataType|paramType|required|defaultValue|minValue|maxValue|allowableValues|allowMultiple|description
	</pre><br>
 The name and datatype are required. All other fields can be left empty.
 The format for the allowableValues if present is a CSV.<br>
 Here are some examples:<br>
 1. A simple header param:<br>
 @implicitParam p1|string|header<br>
 2. An int query param with default min and max values that is required and has a description:<br>
 @implicitParam p2|int|query|true|5|1|10||true|test dec<br>
 3. A string path param with allowable values A and B and a default value of A and a description:
 @implicitParam p3|string|path|true|A|||A,B||test<br>
 <br><b>Note that if you want to add a non primitive e.g. object type as an implicit parameter it must be given the paramType of body.</b>
</td><td>operations, resource class</td><td>@additionalParam,@extraParam</td></tr>
	
	
	<tr><td>@paramsFormat</td><td>Defines the format for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1Format param2Name param2Format. Note that this is only used for types that do not already map to a predefined format; which primarily means string type. NOTE2: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@formats</td></tr>
	
	<tr><td>@paramsMinValue</td><td>Defines the minimum value for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1MinValue param2Name param2MinValue. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@paramsMinimumValue, @minValues</td></tr>
	
	<tr><td>@paramsMaxValue</td><td>Defines the maximum value for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1MaxValue param2Name param2MaxValue. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@paramsMaximumValue, @maxValues</td></tr>
	
	<tr><td>@paramsDefaultValue</td><td>Defines the default value for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1DefaultValue param2Name param2DefaultValue. This doclet also supports reading default values from the JAXRS DefaultValue annotation which takes precedence over this for a given parameter. For enums a default value must be one of the enum values, for numeric types if there is a minimum value then a default value must be >= to it, similarly if there is a maximum value then a default value must be <= to it. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@defaultValues</td></tr>
	
	<tr><td>@paramsAllowableValues</td><td>Defines the allowable values for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1AllowableValue1 param1Name param1AllowableValue2 param2Name param2AllowableValue1 etc. A default value must be one of these allowable values. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@allowableValues</td></tr>
	
	<tr><td>@paramsNameValue</td><td>Defines custom names for one or more of the parameters of an operation. This uses a format of space separated name and value pairs e.g.  param1Name param1CustomName param2Name param2CustomName. NOTE: The values for the name of the parameter in this CSV must be the raw name in the method signature/bean param field not the name as derived via an annotation or javadoc tag.</td><td>operations</td><td>@overrideParamsName</td></tr>
	
	<tr><td>@resourcePath</td><td>This sets the path for resources in the resource listing e.g. the service.json file. This does NOT impact the api path which is soley controlled via the @Path annotations. You should put this tag on either a) the resource class if using a single resource class per api resource, or b) one of the operation methods of each resource if you have endpoints from multiple resources in the same class file. NOTE if you have resource classes with empty paths or a path that is / then by default these classes will be give the resource path of /root, if you put @resourcePath on the class this will be used instead of /root. You can also use the doclet parameter -resourceRootPath to customize the resource path for root resources.</td><td>operations, resource classes</td><td>@resource,@parentEndpointName</td></tr>
	
	<tr><td>@resourceDescription</td><td>This sets the description for an operation in the resource listing e.g. the service.json file. You should put this tag on either the resource class, if using a single resource class per api resource, or one of the operation methods of each resource, if you have endpoints from multiple resources in the same class file.</td><td>operations, resource classes</td><td></td></tr>
	
	<tr><td>@resourcePriority</td><td>This sets a priority for ordering resources in the resource listing e.g. the service.json file. They are ordered in ascending order of priority provided the doclet option -sortResourcesByPriority is set. You should put this tag on either the resource class, if using a single resource class per api resource, or one of the operation methods of each resource, if you have endpoints from multiple resources in the same class file. NOTE this will only take effect if the swagger ui apisSorter option is "none" in the index.html of the swagger ui, this is NOT the case in the embedded swagger ui.</td><td>operations, resource classes</td><td>@resourceOrder,@priority</td></tr>
	
	<tr><td>@unauthorized</td><td>Indicates a method does NOT require authentication, in this case an empty authorizations field will be added to the operation json e.g. authorizations": { } The swagger 1.2 spec indicates this overrides authentication at the api level however in practice it appears that not adding this vs adding empty authorizations has the same effect in Swagger UI.</td><td>operations</td><td>@noAuth</td></tr>
	<tr><td>@scope</td><td>Indicates this method requires authorization and in particular the calling user must have this scope. Multiple scopes can be added using multiple tags. The scopes defined on the operation level must be one of the scopes in the Authorizations section of the service.json</td><td>operations</td><td>@oauth2Scope</td></tr>
	<tr><td>@authentication</td><td>This is an alternative way to define that an endpoint either requires authorization or does not require authorization. It can be set to either @authentication Required or @authentication Not required. If it is set to required then you must also set some default scopes that are to be applied via the -authOperationScopes doclet option.</td><td>operations</td><td>@authorization</td></tr>
	<tr><td>@deprecated</td><td>This defines an operation method or a model field/method as deprecated. By default the doclet will not include these in the generated documentation. This behaviour can be overridden by the -disableDeprecated* options described in the doclet options section below. If a deprecated operation is included then the deprecated field of the operation will be set to "true" as per the swagger 1.2 spec.</td><td>operations, model fields & methods.</td><td></td></tr>
	<tr><td>@Deprecated</td><td>This defines an operation method, parameter or a model field/method as deprecated. By default the doclet will not include these in the generated documentation. This behaviour can be overridden by the -disableDeprecated* options described in the doclet options section below. If a deprecated operation is included then the deprecated field of the operation will be set to "true" as per the swagger 1.2 spec. As this is an annotation vs a javadoc tag it can be applied to operation parameters.</td><td>operations, operation parameters, model fields & methods.</td><td></td></tr>
	
</table>

## Doclet Options

### Mandatory Options

These are the options that you will always want to set

<table>
	<tr><th>Option</th><th>Purpose</th></tr>
	<tr><td>-docBasePath</td><td><p>The base path to the docs in the service.json. You should set to the url of the dir where the swagger json files are hosted e.g. for Carma we use -docBasePath https://api-dev.car.ma/apidoc/ref. If you dont want to build different json files for each server environment one solution which we use at Carma is to use a variable reference for the docBasePath and then serve the json file(s) through a servlet filter which can replace the variable with the hostname of the server it is running on. Alternatively you can use a relative path use the workaround described below (this is technically against the swagger 1.2 spec).</p><p>NOTE: Do not end this with a forward slash.</p>
	<p>NOTE 2: Technically as per the swagger 1.2 spec the doc base path which is in the resource listing file is NOT required, however, if it is not specified the swagger ui will only work if the resource listing file is in one directory and the url in the index.html points to this and the other resource json files are relative to this. This doclet generates the resource listing as a file called service.json and the resource json files are generated in the same directory. You can of course name the files and move them to a structure similar to the swagger petstore example where the resource listing is in at the path /api-docs and the resources are under this such as /api-docs/Users.json or you can use web server rewrites to simulate this structure.</p>
	<p>NOTE 3: For versions of the swagger ui prior to 2.1.0 If you want to use a relative docBasePath then you will need to patch the swagger.js file of the swagger ui to support this. This is because according to the 1.2 spec the docBasePath is supposed to be absolute and earlier versions of the swagger ui did not handle this.</p>
	<p>The patch to apply for relative doc base url is as follows:
<pre>
<code>
SwaggerApi.prototype.buildFromSpec = function(response) {
...
if (response.basePath)
    this.basePath = response.basePath;
  else if (this.url.indexOf('?') > 0)
    this.basePath = this.url.substring(0, this.url.lastIndexOf('?'));
  else
    this.basePath = this.url;
  
  //BEGIN PATCH to support relative base url in the top level resource listing
  if (this.basePath.indexOf('/') == 0) {
    var full = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '');
    this.basePath = full + this.basePath
  }
  //END PATCH to support relative base url in the top level resource listing
  ...
  
  
SwaggerApi.prototype.buildFrom1_1Spec = function(response) {
  ...
if (response.basePath)
    this.basePath = response.basePath;
  else if (this.url.indexOf('?') > 0)
    this.basePath = this.url.substring(0, this.url.lastIndexOf('?'));
  else
    this.basePath = this.url;
  
  //BEGIN PATCH to support relative base url in the top level resource listing
  if (this.basePath.indexOf('/') == 0) {
    var full = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '');
    this.basePath = full + this.basePath
  }
  //END PATCH to support relative base url in the top level resource listing
  ...
  
</code>
</pre>
</p>
</td></tr>
	<tr><td>-apiBasePath</td><td><p>The base path to make api calls via Swagger UI, For each API operation the url to that operation is the apiBasePath + the operation Api path. This can be relative if the swagger json files are on the same host as your API. For Carma we use -apiBasePath /carmaapi</p>
	<p>Note: if both the docBasePath and apiBasePath are relative then for versions of the Swagger UI prior to 2.1.0 you will need to patch the swagger.js file of the swagger ui to support this. This is because according to the 1.2 spec the docBasePath is supposed to be absolute. See the docBasePath description above for details of this patch.</p>
	</td></tr>
	<tr><td>-apiVersion</td><td>The version of your API used in the service.json and your various API json files.</td></tr>
</table>

### Customizable Options

These are the options that you may want to use to add additional functionality or control the doclet behaviour

<table>
	<tr><th>Option</th><th>Purpose</th></tr>
	<tr><td>-d</td><td>The path to the directory where the generated swagger json files should be written e.g. -d /tmp/foo. By default this will be the reportOutputDirectory of the doclet.</td></tr>
	
	<tr><td>-apiAuthorizationsFile</td><td>The path to the json include file that contains the authorizations spec that should be included in the generated json. NOTE wrap the path in single qoutes to escape spaces. For example for Carma we use: 
	<p></p>
	<p>-apiAuthorizationsFile ${project.build.directory}/swagger/includes/apiauth.json</p>
	
	which includes this content:
	
	<code><pre>
	{
  "oauth2": {
    "type": "oauth2",
    "scopes": [
      {
        "scope": "rtr",
        "description": "Standard Carma Scope allowing access to the authenticated user"
      }
    ],
    "grantTypes": {
      "implicit": {
	    "loginEndpoint": {
	          "url": "https://api-dev.car.ma/security/oauth/authorize"
	        },
	        "tokenName": "access_token"
	      }
      }
    }
  }
}
	</pre></code>
	</td></tr>
	
	<tr><td>-apiInfoFile</td><td>The path to the json include file that contains the Info that should be included in the generated json. NOTE wrap the path in single qoutes to escape spaces. For example for Carma we use: 
	<p></p>
	<p>-apiInfoFile ${project.build.directory}/swagger/includes/apiinfo.json</p>
	
	which includes this content:
	
	<code><pre>
{

    "title": "Carma APIs",
    "description": "<p>This is an interactive API reference for the Carma APIs.</p><p>Below you will see the main sections of the API. Click each section in order to see the endpoints that are available in that category and use the 'Try it out' button to make API calls.</p><p>For endpoints that require authentication you can click the on/off toggle in the top right of the endpoint description, which will take you to a login form.</p><p>If you have not set up a Carma user account already you can do so <a href='https://rtr-dev.car.ma/login' target='_blank'>here</a>.",
    "termsOfServiceUrl": "https://api-dev.car.ma/apidoc/terms.action",
    "contact": "carma-apis@car.ma"

}
	</pre></code>
	</td></tr>
	
	
	<tr><td>-extraApiDeclarations</td><td>A CSV of paths to json include files that contains the definitions of extra apis that should be included in the generated json. NOTE wrap the CSV of paths in single qoutes to escape spaces. For example for Carma we use: 
	<p></p>
	<p>-extraApiDeclarations ${project.build.directory}/swagger/includes/oauth.json</p>
	
	which includes this content:
	
	<code><pre>
	{
	"basePath": "/",
    "resourcePath": "/login",
    "apis": [
        {
            "path": "/security/oauth/token/pw",
            "operations": [
                {
                    "method": "POST",
                    "nickname": "getTokenPasswordFlow",
                    "type": "Token",
                    "summary": "Password Authentication",
                    "notes": "This is the Oauth 2.0 Password flow which allows you to provide your client application credentials and the user's login credentials and receive back an Oauth token for the user. This is only available to certain trusted clients.",
                    "parameters": [
                        {
                            "paramType": "query",
                            "name": "client_id",
                            "type": "string",
                            "required" : true
                        },
                        {
                            "paramType": "query",
                            "name": "client_secret",
                            "type": "string",
                            "required" : true
                        },
                        {
                            "paramType": "query",
                            "name": "username",
                            "type": "string",
                            "required" : true
                        },
                        {
                            "paramType": "query",
                            "name": "password",
                            "type": "string",
                            "required" : true
                        },
                        {
                            "paramType": "query",
                            "name": "grant_type",
                            "type": "string",
                            "required" : true,
                            "enum": [
						        "password"
						    	],
						    "defaultValue" : "password"
                        },
                        {
                            "paramType": "query",
                            "name": "scope",
                            "type": "string",
                            "required" : true,
                            "enum": [
						        "rtr",
						        "rtr,rtr-restricted"
						    	],
						    "defaultValue" : "rtr"
                        }
                    ],
                    "produces": ["application/json"],
                    "responseMessages": [
                    	{
						    "code": 200,
						    "message": "Authentication was successful."
						},
						{
						    "code": 401,
						    "message": "Authentication failed."
						}
                    ]
                }                
            ]
        }
    ],
    "models": {
        "Token": {
            "id": "Token",
            "properties": {
                "access_token": {
                    "type": "string"
                },
                "expires_in": {
                    "type": "integer",
                    "format": "int32"
                },
                "uid": {
                    "type": "integer",
                    "format": "int64"
                },
                "scope": {
                    "type": "string"
                },
                "token_type": {
                    "type": "string"
                }
            }
        }
    }
}
		</pre></code>
	</td></tr>
	
	<tr><td>-variablesPropertiesFile</td><td>Path to a properties file that holds variable replacements which can be referenced in java doc. For example if a method description contains ${userFieldNamesDesc} and this properties file had a property set like this:<br>

	
<pre><code>userFieldNamesDesc=The user field names is a csv of fields you can request for a user.</code></pre>
	
Then the variable ${userFieldNamesDesc} would be replaced by the value from the properties file anywhere in the javadoc it was referenced.</td></tr>
	
	<tr><td>-disableCopySwaggerUi or -skipUiFiles</td><td>If set then this does not copy the Swagger UI to the output dir. This can be useful to speed up the doclet and reduce the size of the generated artifact if you either do not use the Swagger UI or host or package it separately.</td></tr>
	
	<tr><td>-swaggerUiPath</td><td>If copying of the Swagger UI is enabled this is the path to the zip file or dir that includes the ui. If not provided then the default Swagger UI embedded in the doclet plugin will be used. If the legacy swaggerUiZipPath option is provided then this will be set to that value.</td></tr>
	
	<tr><td>-disableModels</td><td>This turns off generation of models in the documentation.</td></tr>
	
	<tr><td>-resourceRootPath</td><td>This lets you customize the resource path used for resource classes that have root paths e.g. @Path("/") or @Path(""). By default the resource path used for these will be /root but you can use this or the @resourcePath javadoc tag to customize this.</td></tr>
	
	<tr><td>-disableSortApisByPath</td><td>This is whether to disable sorting of apis inside a resource by their path. If not set then they will be ordered by their path. If set then they will be in the order encountered by the parser. NOTE this will only take effect if the swagger ui apisSorter option is "none" in the index.html of the swagger ui, this is NOT the case in the embedded swagger ui.</td></tr>
	
	<tr><td>-sortResourcesByPriority</td><td>This is whether the resources in the resource listing e.g. service.json are ordered by their priority as defined by the @resourcePriority tags. Resources without a priorty tag are placed at the bottom. This takes precedence over -sortResourcesByPath. If neither this nor the -sortResourcesByPath options are set then they will be listed in the order encountered by the parser. NOTE this will only take effect if the swagger ui apisSorter option is "none" in the index.html of the swagger ui, this is NOT the case in the embedded swagger ui.</td></tr>
	
	<tr><td>-sortResourcesByPath</td><td>This is whether the resources in the resource listing e.g. service.json are ordered by their path. If neither this nor the -sortResourcesByPriority options are set then they will be listed in the order encountered by the parser. NOTE this will only take effect if the swagger ui apisSorter option is "none" in the index.html of the swagger ui, this is NOT the case in the embedded swagger ui.</td></tr>
	
	<tr><td>-disableDeprecatedResourceClassExclusion</td><td>By default resource classes which have either the @deprecated tag or @Deprecated annotation are excluded from the generated documentation. If this flag is set they will be included.</td></tr>
	
	<tr><td>-disableDeprecatedModelClassExclusion</td><td>By default model classes which have either the @deprecated tag or @Deprecated annotation are excluded from the generated documentation. If this flag is set they will be included.</td></tr>
	
	<tr><td>-disableDeprecatedOperationExclusion</td><td>By default operation methods which have either the @deprecated tag or @Deprecated annotation are excluded from the generated documentation. If this flag is set they will be included.</td></tr>
	
	<tr><td>-disableDeprecatedFieldExclusion</td><td>By default model fields which have either the @deprecated tag or @Deprecated annotation (either on the model field or method) are excluded from the generated documentation. If this flag is set they will be included.</td></tr>
	
	<tr><td>-disableDeprecatedParamExclusion</td><td>By default operation parameters which have the @Deprecated annotation are excluded from the generated documentation. If this flag is set they will be included.</td></tr>

	<tr><td>-customDeprecatedAnnotation</td><td>Allows for custom annotations which represent deprecated annotation</td></tr>
	
	<tr><td>-excludeModelPrefixes</td><td>This adds additional classes to the set of model classes that are NOT documented. The default set contains org.joda.time.DateTime, java.util.UUID, java.io. . This supports a full class name as well as prefixes of the fully qualified class names which means you can enter a package like com.foo to exclude all classes under the com.foo package. This replaces the -typesToTreatAsOpaque option but if that option is specified then those classes will be added to the excludeModelPrefixes set.</td></tr>
	
	<tr><td>-excludeResourcePrefixes</td><td>This allows you to exclude resource classes from the generated documentation. It is a CSV and supports a full class name as well as prefixes of the fully qualified class names which means you can enter a package like com.foo to exclude all classes under the com.foo package.</td></tr>
	
	<tr><td>-includeResourcePrefixes</td><td>This allows you to ONLY include resource classes from the generated documentation that match this. It is a CSV and supports a full class name as well as prefixes of the fully qualified class names which means you can enter a package like com.foo to include only classes under the com.foo package. You can use the excludeResourcePrefixes in conjunction with this to exclude specific classes/packages within the included set.</td></tr>
	
	<tr><td>-genericWrapperTypes</td><td>This adds additional classes to the set of model classes that act as genericized wrappers to the actual entity or parameter that should be documented. The default set contains com.sun.jersey.api.JResponse, com.google.common.base.Optional, jersey.repackaged.com.google.common.base.Optional</td></tr>
	
	<tr><td>-modelFieldsRequiredByDefault</td><td>This is whether model fields are required by default e.g. if a model field has neither @optional or @required on it. If you do not set this then model fields are NOT required by default. If you set this option then they ARE required by default.</td></tr>
	
	<tr><td>-disableModelFieldsXmlAccessType</td><td>By default the doclet will use @XmlAccessorType to decide which model fields to generate. To disable this option you can set this parameter which will mean all model fields and getters will generate properties apart from static and transient ones.</td></tr>
	
	<tr><td>-defaultModelFieldsXmlAccessType</td><td>By default the doclet WILL include private fields in generated documentation even if disableModelFieldsXmlAccessType is false. This is because many users expect this behaviour and instead prefer to mark fields to exclude using transient and other javadoc tags. If you DO want default behaviour of fields to be per the JAXB spec e.g. XmlAccessType.PUBLIC_MEMBER then set this flag.</td></tr>
	
	<tr><td>-modelFieldsNamingConvention</td><td>This is an optional naming convention that can be used for the naming of fields of models. If not specified then the fields of models will have the same name as the java field name unless it has one of the annotations that can override the name such as @XmlAttribute, @XmlElement or @JsonProperty. There are 3 types of naming conventions that can be used instead: lower case, upper case, and lower case with underscore separating words. For each of these they can be used either always or only when there is no name customising annotation/tag (such as @XmlAttribute). The supported values for this field are: LOWER_UNDERSCORE, UPPERCASE, LOWERCASE which always take effect or their equivalents which only apply when there is not customised name for the field are: LOWER_UNDERSCORE_UNLESS_OVERRIDDEN, UPPERCASE_UNLESS_OVERRIDDEN, LOWERCASE_UNLESS_OVERRIDDEN, </td></tr>
	
	<tr><td>-useFullModelIds</td><td>By default the ids used for models will be the simple class name, for example a class called foo.bar.Data will have a model id of Data. If you use classes with the same name but different packages then you should enable this option.</td></tr>
	
	<tr><td>-serializationInclusion</td><td>By default this will be NON_NULL, but you can change this to any of the other supported values ALWAYS, NON_DEFAULT, NON_EMPTY. See com.fasterxml.jackson.annotation.JsonInclude$Include for more details.</td></tr>
	
	<tr><td>-defaultTyping</td><td>This is not enabled by default. You can choose to set it to one of the following values: JAVA_LANG_OBJECT, OBJECT_AND_NON_CONCRETE, NON_CONCRETE_AND_ARRAYS, NON_FINAL. See com.fasterxml.jackson.databind.ObjectMapper$DefaultTyping for futher details.</td></tr>
	
	<tr><td>-serializationFeatures</td><td>The default value for this is INDENT_OUTPUT:true. The format of this is a CSV in the form featureName:enabledflag, feature2Name:enabledflag ... where enableflag is true or false. See com.fasterxml.jackson.databind.SerializationFeature for futher details.</td></tr>

	<tr><td>-deserializationFeatures</td><td>There is no default value for this. The format of this is a CSV in the form featureName:enabledflag, feature2Name:enabledflag ... where enableflag is true or false. See com.fasterxml.jackson.databind.DeserializationFeature for futher details.</td></tr>	
	<tr><td>-stringTypePrefixes</td><td>This adds additional prefixes to the list of prefixes of class types that if matched mean the data type used for a given type is always string. The default list contains com.sun.jersey.core.header. which means that custom jersey header classes like com.sun.jersey.core.header.FormDataContentDisposition are given the string data type. If you want custom class types to be treated as string you can add to this csv.</td></tr>
	<tr><td>-longTypePrefixes</td><td>This adds additional prefixes to the list of prefixes of class types that if matched mean the data type used for a given type is always a long. If you want custom class types to be treated as a long you can add to this csv.</td></tr>
	<tr><td>-intTypePrefixes</td><td>This adds additional prefixes to the list of prefixes of class types that if matched mean the data type used for a given type is always an int. If you want custom class types to be treated as an int you can add to this csv.</td></tr>
	<tr><td>-doubleTypePrefixes</td><td>This adds additional prefixes to the list of prefixes of class types that if matched mean the data type used for a given type is always a double. If you want custom class types to be treated as a double you can add to this csv.</td></tr>
	<tr><td>-floatTypePrefixes</td><td>This adds additional prefixes to the list of prefixes of class types that if matched mean the data type used for a given type is always a float. If you want custom class types to be treated as a float you can add to this csv.</td></tr>
	
	<tr><td>-responseMessageSortMode</td><td>This controls how response messages are sorted. This can be one of: 
	<ul>
	<li>CODE_ASC this is the default, it means in ascending order of the HTTP status code so success codes would come before error codes</li>
	<li>CODE_DESC means in descending order of the HTTP status code so error codes would come before success codes</li>
	<li>AS_APPEARS same order as they appear in the javadoc</li>
	</ul>
	</td></tr>
	
	<tr><td>-authOperationScopes</td><td>If an operation has "@authentication required" on it then it needs to know which scopes are required. This default set of scopes can be set via this option.</td></tr>
	
</table>

### Additional Options

These are options that you typically won't need to use unless for example, you want to support different javadoc tags without needing to modify your source code.

<table>
	<tr><th>Option</th><th>Purpose</th></tr>
	
	<tr><td>-excludeParamAnnotations</td><td>This adds additional annotation classes to the set of annotations used to exclude operation parameters from the documentation. The default set contains javax.ws.rs.core.Context, javax.ws.rs.CookieParam, javax.ws.rs.MatrixParam, javax.ws.rs.container.Suspended</td></tr>
	
	<tr><td>-responseMessageTags</td><td>This adds additional tags to the set of javadoc tags used for response messages. The default set contains responseMessage, status, errorResponse, errorCode, successResponse, successCode. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-excludeOperationTags</td><td>This adds additional tags to the set of javadoc tags used for excluding operations. The default set contains exclude, hide, hidden. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-excludeFieldTags</td><td>This adds additional tags to the set of javadoc tags used for excluding model fields/methods. The default set contains exclude, hide, hidden. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-excludeParamsTags</td><td>This adds additional tags to the set of javadoc tags used for excluding operation parameters. The default set contains excludeParams, hiddenParams, hideParams. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-resourceTags</td><td>This adds additional tags to the set of javadoc tags used for setting the resource that an operation is part of. The default set contains resource, resourcePath, parentEndpointName. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-responseTypeTags</td><td>This adds additional tags to the set of javadoc tags used for setting a custom response model for an operation. The default set contains responseType, outputType. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-inputType</td><td>This adds additional tags to the set of javadoc tags used for setting a custom model for an operation parameter. The default set contains inputType, bodyType. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-defaultErrorType</td><td>This adds additional tags to the set of javadoc tags used to the define the model class to be added to response messages for status codes >= 400. The default set contains defaultErrorType. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-apiDescriptionTags</td><td>This adds additional tags to the list of javadoc tags used for setting the description of an api. The default list contains apiDescription. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-operationSummaryTags</td><td>This adds additional tags to the set of javadoc tags used for setting the summary for an operation. The default set contains summary, endpointName. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-operationNotesTags</td><td>This adds additional tags to the set of javadoc tags used for setting the notes for an operation. The default set contains description, notes, comment. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldDescriptionTags</td><td>This adds additional tags to the list of javadoc tags used for setting the description of model field/methods. The default list contains description, comment, return. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldFormatTags</td><td>This adds additional tags to the list of javadoc tags used for setting the format of a model field. The default list contains format. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldMinTags</td><td>This adds additional tags to the list of javadoc tags used for setting the min value of a model field. The default list contains min, minimum. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldMaxTags</td><td>This adds additional tags to the list of javadoc tags used for setting the max value of a model field. The default list contains max, maximum. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldDefaultTags</td><td>This adds additional tags to the list of javadoc tags used for setting the default value of a model field. The default list contains default, defaultValue. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fieldAllowableValuesTags</td><td>This adds additional tags to the list of javadoc tags used for setting the allowable values (a.k.a. enum values) of a model field. The default list contains allowableValues, valies, enum. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-resourcePriorityTags</td><td>This adds additional tags to the list of javadoc tags used for setting the priority of a resource for order in the resource listing/service.json. The default list contains resourcePriority, resourceOrder. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-resourceDescriptionTags</td><td>This adds additional tags to the list of javadoc tags used for setting the description of a resource in the esource listing/service.json. The default list contains resourceDescription. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-operationScopeTags</td><td>This adds additional tags to the list of javadoc tags used for setting an oauth2 scope that is required to access an operation. The default list contains scope, oauth2Scope. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-unauthOperationTags</td><td>This adds additional tags to the list of javadoc tags used for indicating an operation does NOT require auhtorization. The default list contains unauthorized, noAuth. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-authOperationTags</td><td>This adds additional tags to the list of javadoc tags used for the alternative way of indicating whether an operation requires or does not require auhtorization. The default list contains authentication, authorization. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-requiredParamsTags</td><td>This adds additional tags to the list of javadoc tags used for setting whether operation parameters are required. The default list contains requiredParams. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-optionalParamsTags</td><td>This adds additional tags to the list of javadoc tags used for setting whether operation parameters are optional. The default list contains optionalParams. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-csvParamsTags</td><td>This adds additional tags to the list of javadoc tags used for setting whether operation parameters are csv/multi valued. The default list contains csvParams. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-implicitParamTags</td><td>This adds additional tags to the list of javadoc tags used for adding additional resource method parameters. The default list contains implicitParam,additionalParam,extraParam. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-paramsFormatTags</td><td>This adds additional tags to the list of javadoc tags used for setting formats for operation parameters. The default list contains paramsFormat, formats. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-paramsMinValueTags</td><td>This adds additional tags to the list of javadoc tags used for setting minimum values for operation parameters. The default list contains paramsMinValue, paramsMinimumValue, minValues. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-paramsMaxValueTags</td><td>This adds additional tags to the list of javadoc tags used for setting maximum values for operation parameters. The default list contains paramsMaxValue, paramsMaximumValue, maxValues. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-paramsDefaultValueTags</td><td>This adds additional tags to the list of javadoc tags used for setting default values for operation parameters. The default list contains paramsDefaultValue, defaultValues. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-paramsAllowableValuesTags</td><td>This adds additional tags to the list of javadoc tags used for setting allowable values for operation parameters. The default list contains paramsAllowableValues, allowableValues. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-requiredFieldTags</td><td>This adds additional tags to the list of javadoc tags used for setting whether model fields are required. The default list contains required and requiredField. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-optionalFieldTags</td><td>This adds additional tags to the list of javadoc tags used for setting whether model fields are optional. The default list contains optional and optionalField. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-fileParameterAnnotations</td><td>This adds additional annotation classes to the list of annotations that flag a resource parameter as having the File data type. The default list contains org.jboss.resteasy.annotations.providers.multipart.MultipartForm. This list only applies when the resource has multipart/form-data as its @Consumes</td></tr>
	
	<tr><td>-fileParameterTypes</td><td>This adds additional classes to the list of parameter type classes that flag a resource parameter as having the File data type. The default list contains java.io.File, java.io.InputStream, byte[], org.springframework.web.multipart.commons.CommonsMultipartFile, org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput. This list only applies when the resource has multipart/form-data as its @Consumes</td></tr>
	
	<tr><td>-formParameterAnnotations</td><td>This adds additional annotation classes to the list of annotations that flag a resource parameter as having the form parameter type. The default list contains com.sun.jersey.multipart.FormDataParam, javax.ws.rs.FormParam.</td></tr>
	
	<tr><td>-formParameterTypes</td><td>This adds additional classes to the list of parameter type classes that flag a resource parameter as having the form parameter type. The default list contains com.sun.jersey.core.header.FormDataContentDisposition.</td></tr>
	
	<tr><td>-parameterNameAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used for the name of resource parameters. These take precedence over the default parameter name from the method signature. They are superceded by any of the paramsNameTags javadoc tags for a given parameter.</td></tr>
	
	<tr><td>-paramsNameTags</td><td>This adds additional tags to the list of javadoc tags used for setting custom names for parameters. These supercede both the default parameter name from the method signature as well as any annotations used for the parameter name. The default list contains paramsName and overrideParamsName. NOTE: The values in the doclet option should NOT have the @ symbol on them.</td></tr>
	
	<tr><td>-compositeParamAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to denote a parameter class as being a composite parameter class. The default list contains javax.ws.rs.BeanParam.</td></tr>
	
	<tr><td>-compositeParamTypes</td><td>This adds additional classes to the list of parameter type classes that are used to denote a parameter class as being a composite parameter class. The default list is empty.</td></tr>
	
	<tr><td>-discriminatorAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to denote the name of a model's discriminator property. If the model does not have a property with this name specified then it will be automatically be added as a string type model property. The default list contains com.fasterxml.jackson.annotation.JsonTypeInfo.</td></tr>
	
	<tr><td>-subTypesAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to denote sub types of a model class. The default list contains com.fasterxml.jackson.annotation.JsonSubTypes.</td></tr>
	
	<tr><td>-paramMinValueAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a min value for a parameter. The default list contains javax.validation.constraints.Size and javax.validation.constraints.DecimalMin.</td></tr>
	
	<tr><td>-paramMaxValueAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a max value for a parameter. The default list contains javax.validation.constraints.Size and javax.validation.constraints.DecimalMax.</td></tr>
	
	<tr><td>-fieldMinAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a min value for a model field. The default list contains javax.validation.constraints.Size and javax.validation.constraints.DecimalMin.</td></tr>
	
	<tr><td>-fieldMaxAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a max value for a model field. The default list contains javax.validation.constraints.Size and javax.validation.constraints.DecimalMax.</td></tr>
	
	<tr><td>-requiredParamAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a parameter is required. The default list contains javax.validation.constraints.NotNull.</td></tr>
	
	<tr><td>-optionalParamAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a parameter is optional. The default list contains javax.validation.constraints.Null.</td></tr>
	
	<tr><td>-requiredFieldAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a model field is required. The default list contains javax.validation.constraints.NotNull.</td></tr>
	
	<tr><td>-optionalFieldAnnotations</td><td>This adds additional annotation classes to the list of annotations that are used to specify a model field is optional. The default list contains javax.validation.constraints.Null.</td></tr>
	
	<tr><td>-logDebug</td><td>This turns on debug level logging which gives more verbose output.</td></tr>

</table>


<a name="example"></a>
## Example

An example project using Dropwizard is included in `swagger-doclet-sample-dropwizard`. To get it running, run the following commands.

```
$ cd swagger-doclet-sample-dropwizard
$ mvn package
$ java -jar target/swagger-doclet-sample-dropwizard*.jar server sample.yml
```

The example server should be running on port 7070:

You can view the Swagger UI running here: [http://127.0.0.1:7070/apidocs/](http://127.0.0.1:7070/apidocs/)
NOTE you need to add the trailing forward slash for the CSS to load.

You can also inspect the generated json:

```
$ curl localhost:7070/apidocs/service.json
{
  "swaggerVersion" : "1.2",
  "apiVersion" : "1",
  "basePath" : "http://127.0.0.1:7070/apidocs",
  "apis" : [ {
    "path" : "/Response.{format}"
  }, {
    "path" : "/Recursive.{format}"
  }, {
    "path" : "/person.{format}"
  }, {
    "path" : "/parent.{format}"
  }, {
    "path" : "/ModelResource_modelid.{format}"
  }, {
    "path" : "/HttpServletRequest.{format}"
  }, {
    "path" : "/greetings_name.{format}"
  }, {
    "path" : "/Auth.{format}"
  } ]
}
$
```

## Override Swagger UI

To override the Swagger UI included with the doclet, you can either use your own zip or your own directory and add the swaggerUiPath option to the additionalparam attribute in the pom file.

Here is an example pointing to a zip file:

```
<additionalparam>-apiVersion 1 -docBasePath /apidocs -apiBasePath / -swaggerUiPath ../../../src/main/resources/swagger-ui.zip</additionalparam>
```

Here is an example pointing to a directory:

```
<additionalparam>-apiVersion 1 -docBasePath /apidocs  -apiBasePath / -swaggerUiPath ../../../src/main/resources/swagger-ui/</additionalparam>
```
