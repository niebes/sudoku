// The site: the static assets in site/ joined by the solver :lib compiles to JavaScript.
// `assemble` (or `build`) lays the deployable site out in build/site - open its index.html.
plugins {
    base
}

val site by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("site"))
    from(project(":lib").tasks.named("jsBrowserDistribution"))
    into(layout.buildDirectory.dir("site"))
}

tasks.assemble {
    dependsOn(site)
}
