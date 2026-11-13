File projectDir = new File(request.getOutputDirectory(), request.getArtifactId())
File runScript = new File(projectDir, "run-aura.sh")
if (!runScript.exists()) {
    runScript = new File(request.getOutputDirectory(), "run-aura.sh")
}
if (runScript.exists()) {
    boolean success = runScript.setExecutable(true, false)
    if (!success) {
        System.err.println("[WARN] Could not set executable permission on " + runScript.getAbsolutePath())
    }
}
