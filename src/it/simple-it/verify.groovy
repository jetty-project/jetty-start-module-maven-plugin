File gcloudDatastore = new File( basedir, "target/mod-files/modules/gcloud-datastore.mod" );
assert gcloudDatastore.isFile()
gcloudDatastoreContent = gcloudDatastore.text
assert gcloudDatastoreContent.contains( "maven://com.google.cloud/google-cloud-datastore/1.105.0|lib/gcloud/google-cloud-datastore-1.105.0.jar" )

File conf = new File(basedir,"target/simple-it-gcloud-mod-1.0-SNAPSHOT-config.jar")
assert conf.exists()
java.util.jar.JarFile configFile = new java.util.jar.JarFile( conf, false)
assert configFile.getEntry('modules/gcloud/index.yaml') != null
assert configFile.getEntry('modules/gcloud-datastore.mod') != null
assert configFile.getEntry('modules/gcloud.mod') != null
