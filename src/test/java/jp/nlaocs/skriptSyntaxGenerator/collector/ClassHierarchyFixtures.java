package jp.nlaocs.skriptSyntaxGenerator.collector;

final class MissingDependencyFixture {
}

final class BrokenMethodOwnerFixture {
    public MissingDependencyFixture missing(MissingDependencyFixture value) {
        return value;
    }
}
