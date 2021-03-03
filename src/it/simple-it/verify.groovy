File gcloudDatastore = new File( basedir, "target/mod-files/modules/gcloud-datastore.mod" );
assert gcloudDatastore.isFile()
gcloudDatastoreContent = gcloudDatastore.text
assert gcloudDatastoreContent.contains( "maven://com.google.cloud/google-cloud-datastore/1.105.0|lib/gcloud/google-cloud-datastore-1.105.0.jar" )


def configFile = new java.util.jar.JarFile( new File(basedir,"target/simple-it-unixsocket-mod-1.0-SNAPSHOT-config.jar"), false)
assert configFile.getEntry('modules/gcloud/index.yaml') != null
assert configFile.getEntry('modules/gcloud-datastore.mod') != null
assert configFile.getEntry('modules/gcloud.mod') != null
