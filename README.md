# Continuous Delivery in an Ephemeral World

## Introduction

[Symphonia](https://www.symphonia.io) co-founder [John Chapin](https://twitter.com/johnchapin) offers an overview of existing cloud-native build systems and outlines their benefits and drawbacks, including cost efficiency, environment stability, and provenance.

Drawing on a real-world use case involving a Maven-based build and a serverless application architecture, John walks you step-by-step through the various stages of a naive continuous delivery pipeline, including source control integration, artifact builds, and an infrastructure-as-code-style deployment.

John concludes by demonstrating an optimized continuous delivery pipeline that incorporates advanced CodePipeline and CodeBuild techniques to reduce the overall build time and shrink the continuous delivery feedback loop while still realizing all of the benefits of an on-demand, ephemeral build system.

## Prerequisites

1. An AWS account with full access to all services (required).
1. The appropriate AWS keys set up to use the command line interface (required for git).
1. Git installed on your machine, and configured correctly to interact with
   CodeCommit in your AWS account (required).
1. Java 8 and Maven, installed on your machine (optional).

## Instructions

As this is a tutorial instead of a workshop, don't worry about trying to keep
up during the various demo / live coding segments. You can work through the
tutorial later from this repository.

We'll be using CloudFormation throughout the tutorial - you can either use the web console to deploy the CloudFormation stacks, or you can do it from the CLI as described in each of the phases.

Each phase of the tutorial should be executed in sequence.

## Phase 1 - CodeCommit

Create a CodeCommit repository in your AWS account. This is where we'll check
in our Serverless application code.

```
$ cd sacon-london-2018-tutorial
$ cd phase1
$ aws cloudformation create-stack \
    --region eu-west-2 \
    --stack-name repository \
    --template-body file://repository-cfn.yml
```

By default, the repository will be named `serverless-application`. If you change that, make sure to use the new name in the other commands in this tutorial.
 
Follow the instructions [here](https://docs.aws.amazon.com/codecommit/latest/userguide/how-to-connect.html) to access the repository using `git`.

Now, let's push the Serverless application code to the our new Git repository.

```
$ cd ../application
$ git init
$ git remote add origin \
    https://git-codecommit.eu-west-2.amazonaws.com/v1/repos/serverless-application
$ git commit -am "Initial commit"
$ git push --set-upstream origin master
``` 

Go check out the code in the AWS CodeCommit [web console](https://console.aws.amazon.com/codecommit/home).

## Phase 2 - Can't Someone Else Build It?

This phase involves using CodeBuild to remotely build our Serverless application code. We're going to name the CloudFormation stack `build-pipeline`, because that's what it will eventually be, but we're not there yet.

Notice that the Serverless application code contains a `buildspec.yml` file. That file contains instructions for CodeBuild to run Maven to build our application code.

```
$ cd ../phase2
$ aws cloudformation create-stack \
    --region eu-west-2 \
    --stack-name build-pipeline \
    --template-body file://build-pipeline-cfn.yml \
    --capabilities CAPABILITY_IAM
```

Go explore the AWS CodeBuild [web console](https://console.aws.amazon.com/codebuild/home).

Note that while this approach allows us to build our Serverless application remotely, the process isn't triggered automatically on new commits. We don't quite have an automated continuous integration pipeline.

## Phase 3 - Finally, Continuous Integration!

In this phase, we'll set up a CodePipeline pipeline to automatically trigger our build whenever a new commit is made to our Git repository.

Our new pipeline includes two stages; `Source` and `Build`, each of which contains a single action. Note that we're using an infrastructure-as-code approach *even in our build pipeline.*

```
$ cd ../phase3
$ aws cloudformation update-stack \
    --region eu-west-2 \
    --stack-name build-pipeline \
    --template-body file://build-pipeline-cfn.yml \
    --capabilities CAPABILITY_IAM
```

Observe that your new pipeline kicked off automatically, and you can see the different stages in the CodePipeline [web console](https://console.aws.amazon.com/codepipeline/home).

Now we have an automated continuous integration pipeline, but we're still not deploying our application (and its infrastructure).

## Phase 4 - The Bliss of Continuous Delivery

In this phase, we'll go from automated continuous integration to truly continuous delivery, by using CodePipeline's built-in CloudFormation support, and the [Serverless Application Model](https://github.com/awslabs/serverless-application-model).

```
$ cd ../phase4
$ aws cloudformation update-stack \
    --region eu-west-2 \
    --stack-name build-pipeline \
    --template-body file://build-pipeline-cfn.yml \
    --capabilities CAPABILITY_IAM
```

After updating the stack, we'll also need to update our Serverless application's `buildspec.yml` file to include the `cloudformation package` command, and push those changes to the Git repository.

```
$ cd ../application
$ cp ../phase4/buildspec.yml .
$ git commit -am "Added package command to buildspec.yml"
$ git push
```

After the pipeline runs, notice we have a new CloudFormation stack, `serverless-application`! Feel free to go play with the Lambda functions via the [web console](https://console.aws.amazon.com/lambda/home).

We now have a complete continuous delivery pipeline. Every commit made to the `master` branch of our Git repository will be automatically deployed.

## Phase 5 - Got Cache?

This phase involves speeding up our builds using a <del>highly-customized Docker image</del> simple CodeBuild configuration option. We can ask CodeBuild to cache certain paths using S3, which will pay dividends on subsequent build runs.

```
$ cd ../phase5
$ aws cloudformation update-stack \
    --region eu-west-2 \
    --stack-name build-pipeline \
    --template-body file://build-pipeline-cfn.yml \
    --capabilities CAPABILITY_IAM
```

This also requires an update to our `buildspec.yml`.

```
$ cd ../application
$ cp ../phase5/buildspec.yml .
$ git commit -am "Added cache section to buildspec.yml"
$ git push
```

To see the positive outcome of this change, we'll need to run the build a couple of times. Do that from the CodePipeline [web console](https://console.aws.amazon.com/codepipeline/home).

## Phase 6 - Person in the middle

In this phase, we introduce a manual approval step, which will ensure that a highly reliable, logical, and even-tempered human will have an opportunity to weigh in on whether our build should proceed.

We have to provide our email address, which will be used as a subscription target of an SNS topic. Watch your email for a confirmation!

```
$ cd ../phase6
$ aws cloudformation update-stack \
    --region eu-west-2 \
    --stack-name build-pipeline \
    --template-body file://build-pipeline-cfn.yml \
    --capabilities CAPABILITY_IAM \
    --parameters ParameterKey=Email,ParameterValue=YOUR_EMAIL_ADDRESS
```

Now, our pipeline will pause before deploying, so we can investigate the CloudFormation changeset and either allow the deployment to proceed, or stop it in case there's a problem.
