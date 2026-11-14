allprojects {
  group = "com.copitoB.mypos"
  version = "1.0.0"
}

tasks.register("clean") {
  delete(rootProject.buildDir)
}
