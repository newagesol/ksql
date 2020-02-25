# KLIP-22 - Add consumer group id to CREATE STREAM and CREATE TABLE DSL

**Author**: [Ievgenii Shepeliuk](https://github.com/eshepelyuk) | 
**Release Target**: TBD | 
**Status**: In Discussion | 
**Discussion**: 

**tl;dr:** Having possbility to set fixed consumer group id for KSQL streams will allow customers to ALTER or re CREATE streams 
            without losing data or a need to reprocess data from the beginning.
            
## Motivation and background

Inspired by several similar issues, like

* [confluentinc/ksql#2440](https://github.com/confluentinc/ksql/issues/2440)
* [confluentinc/ksql#2087](https://github.com/confluentinc/ksql/issues/2087)

the issue comes to play when someone need to ALTER existing stream or table.
Since KSQL doesn't provide ALTER command yet, the only way to achive is DROP and re CREATE stream.
During this operation the consumer offset is lost, since currently consumer group id is auto generated during CREATE.

The loss of offset can cause following issues to upstream services that may consume topics dependent on altered stream:

* loss of data, if during recreation some messages were produced to source topic, so new stream will start consuming from latest offest, ignoring those message and affecting upstream service
* the source topic maybe forced to re-read by newly created stream using `earliest` offset, but this obligates upstream service to reprocess data that may be inacceptable

## What is in scope

* Change KSQL DSL to allow passing optional paramter `CONSUMER_GROUP_ID` to `WITH` clause
* When creating KSQL stream or table, detect if consumer group is passed and use it, fallback to current behaviour if empty
* Add more tests cases for a new feature

## What is not in scope

We will not going to resolve general issue of altering KSQL streams, 
since it has much more implications regarding _stateful_ streams that keeps state in tables.
So, in fact, for `CREATE TABLE` this feature will not work as expected, 
until there will be a possibility of not dropping table data while recreating.
As far as I know there're other klips discussed internally.

## Value/Return

Existing users will obtain a possibility of easier operations when altering _stateless_ streams.
They just could DROP / CREATE a stream with the same consumer group id and be safe about the fact that no data was lost.
And no additional operations activity will be required for such use cases.

## Public APIS

Followng pages will be modified 

* [CREATE STREAM](https://docs.confluent.io/current/ksql/docs/developer-guide/syntax-reference.html#create-stream)
* [CREATE TABLE](https://docs.confluent.io/current/ksql/docs/developer-guide/syntax-reference.html#create-table)

Changes will affect the table with description of `WITH` clause

Property| Decription
------------ | -------------
CONSUMER_GROUP_ID | Group if assigned to a consumer that will be created for this stream

## Design

### Improve parsing of WITH clause

* Add new parameter `CONSUMER_GROUP_ID` as a static constant to `io.confluent.ksql.properties.with.CreateConfigs`
* Add static initialization for new parameter via `io.confluent.ksql.properties.with.CreateConfigs#CONFIG_DEF`
* Expose new method `getConsumerGroupId()` in `io.confluent.ksql.parser.properties.with.CreateSourceProperties`

### Create consumer with fixed group id 

TBD ???

_How does your solution work? This should cover the main data and control flows that are changing._

## Test plan

* Create a stream with fixed group id, DROP the stream, create new one with same group id, ensure stream reads offset from previous group id
* Create a stream with fixed group id, DROP the stream, create new one without group id, stream reads records from `latest` offset

## LOEs and Delivery Milestones

TBD ???

Initial proposal looks like

* Implement code and unit tests for `CREATE TABLE` , `CREATE STREAM`
* Functional tests and release

## Documentation Updates

Documentation should reflect that `WITH` clause of `CREATE STREAM` and `CREATE TABLE` command can accept optional `CONSUMER_GROUP_ID` parameter.

## Compatibility Implications

None.

## Security Implications

None.
