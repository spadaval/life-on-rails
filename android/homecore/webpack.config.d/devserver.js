// web-dev.sh's server: loopback only, on a port of its own (not lor-web's 3847-3849, the test
// servers' 3857 and 3858, nor the dashboard prototype's 3859). A rebuilt bundle reloads the page.
if (config.devServer) {
    config.devServer.host = "127.0.0.1";
    config.devServer.port = Number(process.env.LOR_LAB_PORT || 3860);
    config.devServer.open = false;
    config.devServer.liveReload = true;
    config.devServer.hot = false;
    // Kotlin adds the Gradle project's own folder to what the server serves; keep only build outputs.
    const statics = [].concat(config.devServer.static || []);
    config.devServer.static = statics.filter((s) => String(typeof s === "string" ? s : s.directory).includes("/build/"));
}
