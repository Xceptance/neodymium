File projectDir = new File(basedir, "project/my-test-suite")
if (!projectDir.exists()) {
    projectDir = basedir
}

File gitignore = new File(projectDir, ".gitignore")
assert gitignore.exists() : ".gitignore must exist in generated project"

File rawGitignore = new File(projectDir, "gitignore")
assert !rawGitignore.exists() : "Temporary gitignore file must have been renamed to .gitignore"

File tokenGitignore = new File(projectDir, "__gitignore__")
assert !tokenGitignore.exists() : "__gitignore__ token file must not exist in generated project"

File runScript = new File(projectDir, "run-aura.sh")
assert runScript.exists() : "run-aura.sh must exist"
assert runScript.canExecute() : "run-aura.sh must be executable"
