# LimsRest
The restful service used by IGO and IGO customers.

## Dev 
Please update properties files as the populated files are not uploaded to this repo. If you are updating any properties, 
please commit an unassigned value of that property to the relevant properties files in `local`, `dev`, and `production`
### Local Setup
1. Copy properties file from resource path at the LIMS rest host being deployed to
    ```
    # TODO - Find resource path
    rsc_path=               # e.g. ".../path/to/sapio/lims/tomcat/webapps/LimsRest##-1.17.3d/WEB-INF"
    your_host=$(hostname)
    tgt_path=$(pwd)/src/local/resources
    
    files="app.properties connect.txt limsrestcredentials.properties"
    for f in ${files}; do
        scp your_host:src/local/resources/classes/${f} ${your_host}:${tgt_path}
    done
    ```

2. Copy over the files pointed to by these fields in `app.properties` 
    * `nats.keystore_path`
    * `nats.truststore_path`
    
    (Optional - point to a log file)
    * `metadb.publishing_failures_filepath`

3. Update `connect.txt`
    * `lims.host` - Add hostname properties files were copied from
    * `lims.user1`/`lims.user2` - Add personal API user (e.g. api-streid)
    * `lims.pword1`/`lims.pword2` - Add personal API user password` 

4. Modify `gradle.properties` so the target points to dev
    ```
    nexusUrl=
    nexusUrlReleases=
    nexusUsername=
    nexusPassword=
    target=dev
    ```

5. (`gradle clean` &) `gradle bootRun`

## Deploy
1. Copy properties file (see [Local Setup, Step 1](#local-setup))
2. Modify `gradle.properties` so point to prod (see [Local Setup, Step 4](#local-setup))
3. `gradle clean` and `gradle build`