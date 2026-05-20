package dev.erudites.mods.koreanify.client.ime;

public sealed interface PreeditResult {

    PreeditResult UNCHANGED = new Unchanged();

    record Unchanged() implements PreeditResult {}

    record Notify(String value) implements PreeditResult {}

    record Commit(String text) implements PreeditResult {}
}
