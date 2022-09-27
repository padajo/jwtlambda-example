# Example JWT Lambda Function

This is an example application consisting of an AWS Lambda Function that decodes and creates the signature for a JWT.

The input is an SQS event (an SQS queue trigger) where the body is the JWT.

## Simulating a real world cold start

To simulate more of a real world scenario, there is a request to Systems Manager for a parameter called `jwt-example-secret`.

This is not the right way to go about things of course, and this would be handled in a different way, but is indicative of how a secret should be stored outside of a Lambda function. 

To create this parameter, simply use the `aws ssm put-parameter` AWS CLI command. More in-depth info can be found [on this page in the AWS documentation](https://docs.aws.amazon.com/systems-manager/latest/userguide/param-create-cli.html) or by typing `aws ssm put-paramter help` in the AWS CLI.

SSM can also create `SecureString` paramters that use the default KMS key, a customer managed key, or a custom AWS KMS key. Then the key is encrypted



