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

File gitignore = new File(projectDir, "gitignore")
if (!gitignore.exists()) {
    gitignore = new File(request.getOutputDirectory(), "gitignore")
}
if (gitignore.exists()) {
    File targetGitignore = new File(gitignore.getParentFile(), ".gitignore")
    if (!targetGitignore.exists()) {
        boolean renamed = gitignore.renameTo(targetGitignore)
        if (!renamed) {
            System.err.println("[WARN] Could not rename gitignore to .gitignore in " + gitignore.getParent())
        }
    }
}
