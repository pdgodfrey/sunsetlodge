plugins {
  id("com.github.node-gradle.node")
}


val yarn = tasks.named("yarn")

tasks.register<com.github.gradle.node.yarn.task.YarnTask>("devVueApp") {
  dependsOn("yarn")
  args.set(listOf("dev"))
  inputs.file("package.json")
  inputs.file("yarn.lock")
}

tasks.register<com.github.gradle.node.yarn.task.YarnTask>("buildVueApp") {
  dependsOn("yarn")
  args.set(listOf("build"))
  inputs.file("package.json")
  inputs.file("yarn.lock")
}
