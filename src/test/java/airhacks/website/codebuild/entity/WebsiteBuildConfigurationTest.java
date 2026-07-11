package airhacks.website.codebuild.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WebsiteBuildConfigurationTest {

    static final String BUCKET = "airhacks.live";
    static final String DISTRIBUTION_ID = "E123456789";

    static List<String> commands() {
        return WebsiteBuildConfiguration.commands(BUCKET, DISTRIBUTION_ID);
    }

    static int indexOfCommandContaining(List<String> commands, String fragment) {
        return IntStream.range(0, commands.size())
                .filter(index -> commands.get(index).contains(fragment))
                .findFirst()
                .orElse(-1);
    }

    static Stream<Arguments> r1PublishOnlyThePublicDirectory() {
        return Stream.of(
                Arguments.of("R1.1 publishes exclusively the contents of the public directory",
                        (Consumer<List<String>>) commands -> {
                            assertThat(commands).anyMatch(command -> command.contains("aws s3 cp public s3://" + BUCKET));
                            assertThat(commands).noneMatch(command -> command.contains("aws s3 cp . "));
                        }),
                Arguments.of("R1.2 checks for the public directory before modifying the website bucket",
                        (Consumer<List<String>>) commands -> {
                            var existenceCheck = indexOfCommandContaining(commands, "test -d public");
                            var emptyBucket = indexOfCommandContaining(commands, "aws s3 rm");
                            assertThat(existenceCheck).isNotNegative();
                            assertThat(existenceCheck).isLessThan(emptyBucket);
                        }),
                Arguments.of("R1.3 empties the website bucket before copying",
                        (Consumer<List<String>>) commands -> {
                            var emptyBucket = indexOfCommandContaining(commands, "aws s3 rm");
                            var copy = indexOfCommandContaining(commands, "aws s3 cp public");
                            assertThat(emptyBucket).isNotNegative();
                            assertThat(emptyBucket).isLessThan(copy);
                        }),
                Arguments.of("R1.4 invalidates the content distribution after the copy",
                        (Consumer<List<String>>) commands -> {
                            var copy = indexOfCommandContaining(commands, "aws s3 cp public");
                            var invalidation = indexOfCommandContaining(commands, "create-invalidation --distribution-id " + DISTRIBUTION_ID);
                            assertThat(copy).isNotNegative();
                            assertThat(copy).isLessThan(invalidation);
                        }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void r1PublishOnlyThePublicDirectory(String requirement, Consumer<List<String>> assertion) {
        assertion.accept(commands());
    }
}
