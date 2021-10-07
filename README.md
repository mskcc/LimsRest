# LimsRest
The restful service used by IGO and IGO customers.

## Dev Notes
Please update properties files as the populated files are not uploaded to this repo.
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

3. (`gradle clean` &) `gradle bootRun`