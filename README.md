# octopus-artifactory-automation

## Prerequisites
Teamcity parameters listed below should be accessible on level of meta-runner usage  
- ARTIFACTORY_URL
- ARTIFACTORY_USER + ARTIFACTORY_PASSWORD or ARTIFACTORY_TOKEN

Meta-runner 'Push and publish Artifactory Docker Image' depends on 'jfrog' cli utility. The utility must be accessible on using agents