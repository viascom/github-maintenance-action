# Github-Action-Kotlin: Greeter

* https://docs.github.com/en/actions/creating-actions
* https://docs.github.com/en/actions/learn-github-actions/variables

    docker build --tag io.botscripter/github-action-kotlin:1.0.0 .
    docker run -e "INPUT_WHO_TO_GREET=Nikola" io.botscripter/github-action-kotlin:1.0.0 "/opt/action/entrypoint.sh" "NIKOLA"
    docker run -it io.botscripter/github-action-kotlin:1.0.0 sh
