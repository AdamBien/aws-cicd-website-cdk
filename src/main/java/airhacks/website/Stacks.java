package airhacks.website;

import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
/**
 * cdk bootstrap aws://594818620080/eu-central-1
 * cdk bootstrap aws://594818620080/us-east-1
 */
public interface Stacks {
      final static StackProps US_EAST_1 = StackProps.builder()
            .env(Environment.builder()
                    .region("us-east-1").build())
            .build();

      final static StackProps EU_CENTRAL_1 = StackProps.builder()
            .crossRegionReferences(true)
            .env(Environment.builder()
                    .region("eu-central-1").build())
            .build();            
}
