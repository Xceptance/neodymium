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

File runScript = new File(projectDir, "run-neodymium.sh")
assert runScript.exists() : "run-neodymium.sh must exist"
assert runScript.canExecute() : "run-neodymium.sh must be executable"


