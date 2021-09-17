package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eol.globi.server.QueryType;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RequestHelperTest {

    @Test
    public void parseSearch() {
        Map<String, String[]> paramMap = new HashMap<>();
        List<LatLng> latLngs = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(latLngs.size(), Is.is(0));
    }

    @Test
    public void parseSearchSinglePoint() {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("lat", new String[]{"12.2"});
        paramMap.put("lng", new String[]{"12.1"});
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(1));
        assertThat(points.get(0).getLat(), Is.is(12.2d));
        assertThat(points.get(0).getLng(), Is.is(12.1d));

        StringBuilder clause = new StringBuilder();
        RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "), Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc " +
                "WHERE exists(loc.latitude) AND exists(loc.longitude)" +
                " AND loc.latitude = 12.2" +
                " AND loc.longitude = 12.1"));
    }

    private void assertLocationQuery(Map<String, String[]> paramMap) {
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(2));
        assertThat(points.get(0).getLat(), Is.is(10d));
        assertThat(points.get(0).getLng(), Is.is(-20d));
        assertThat(points.get(1).getLat(), Is.is(-10d));
        assertThat(points.get(1).getLng(), Is.is(20d));

        StringBuilder clause = new StringBuilder();
        RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "),
                Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc WHERE" +
                        " exists(loc.latitude)" +
                        " AND exists(loc.longitude)" +
                        " AND loc.latitude < 10.0" +
                        " AND loc.longitude > -20.0" +
                        " AND loc.latitude > -10.0" +
                        " AND loc.longitude < 20.0"));
    }

    @Test
    public void buildCypherSpatialQueryClause() {
        /*
         * from http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_2
         * and https://github.com/globalbioticinteractions/globalbioticinteractions/issues/10
         *           N <---
         *                  |
         *
         *    W             E
         *
         *    |             ^
         *    |             |
         *      ---> S -----
         *
         * assuming that four points make a rectangle
         */
        Map<String, String[]> paramMap = new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"-20,-10,20,10"});
            }
        };
        assertLocationQuery(paramMap);
    }

    @Test
    public void buildCypherSpatialQueryClausePoint() {
        /*
         * from http://www.opensearch.org/Specifications/OpenSearch/Extensions/Geo/1.0/Draft_2
         */
        Map<String, String[]> paramMap = new HashMap<String, String[]>() {
            {
                put("g", new String[]{"POINT(10.0 12.4)"});
            }
        };
        List<LatLng> points = RequestHelper.parseSpatialSearchParams(paramMap);
        assertThat(points.size(), Is.is(1));
        assertThat(points.get(0).getLat(), Is.is(10.0));
        assertThat(points.get(0).getLng(), Is.is(12.4d));

        StringBuilder clause = new StringBuilder();
        RequestHelper.addSpatialClause(points, clause, QueryType.MULTI_TAXON_ALL);
        assertThat(clause.toString().trim().replaceAll("\\s+", " "),
                Is.is(", sourceSpecimen-[:COLLECTED_AT]->loc WHERE" +
                        " exists(loc.latitude)" +
                        " AND exists(loc.longitude)" +
                        " AND loc.latitude = 10.0" +
                        " AND loc.longitude = 12.4"));
    }

    @Test(expected = NumberFormatException.class)
    public void buildInvalidPointGeometry() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("g", new String[]{"POINT(bla blah)"});
            }
        });
    }

    @Test
    public void invalidBoundingBoxParams() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"this ain't no bounding box"});
            }
        });
    }

    @Test(expected = NumberFormatException.class)
    public void invalidBadCoordinatesBoundingBoxParams() {
        RequestHelper.parseSpatialSearchParams(new HashMap<String, String[]>() {
            {
                put("bbox", new String[]{"a,b,c,d"});
            }
        });
    }

    @Test
    public void emptyData() throws IOException {
        String response = "{\"columns\":[\"source_taxon_external_id\",\"source_taxon_name\",\"source_taxon_path\",\"source_specimen_life_stage\",\"source_specimen_basis_of_record\",\"interaction_type\",\"target_taxon_external_id\",\"target_taxon_name\",\"target_taxon_path\",\"target_specimen_life_stage\",\"target_specimen_basis_of_record\",\"latitude\",\"longitude\",\"study_title\"],\"data\":[]}";
        assertThat(RequestHelper.nonEmptyData(response), Is.is(false));
    }

    @Test
    public void nonEmptyData() throws IOException {
        String response = "{\"columns\":[\"source_taxon_external_id\",\"source_taxon_name\",\"source_taxon_path\",\"source_specimen_life_stage\",\"source_specimen_basis_of_record\",\"interaction_type\",\"target_taxon_external_id\",\"target_taxon_name\",\"target_taxon_path\",\"target_specimen_life_stage\",\"target_specimen_basis_of_record\",\"latitude\",\"longitude\",\"study_title\"],\"data\":[[\"EOL_V2:328629\",\"Phoca vitulina\",\"Animalia | Chordata | Mammalia | Carnivora | Phocidae | Phoca | Phoca vitulina\",null,\"PreservedSpecimen\",\"hasParasite\",\"EOL:2857069\",\"Ascarididae\",\"Animalia | Nematoda | Secernentea | Ascaridida | Ascarididae\",null,\"PreservedSpecimen\",null,null,\"http://arctos.database.museum/guid/MSB:Para:1678\"]]}";
        assertThat(RequestHelper.nonEmptyData(response), Is.is(true));
    }

    @Test(expected = IOException.class)
    public void errorCode() throws IOException {
        String errorString = getErrorResult();

        RequestHelper.throwOnError(errorString);
    }

    public static String getErrorResult() {
        return "{\n" +
                "  \"results\" : [ ],\n" +
                "  \"errors\" : [ {\n" +
                "    \"code\" : \"Neo.DatabaseError.Statement.ExecutionFailed\",\n" +
                "    \"message\" : \"this IndexReader is closed\",\n" +
                "    \"stackTrace\" : \"org.apache.lucene.store.AlreadyClosedException: this IndexReader is closed\\n\\tat org.apache.lucene.index.IndexReader.ensureOpen(IndexReader.java:274)\\n\\tat org.apache.lucene.index.SegmentReader.getLiveDocs(SegmentReader.java:167)\\n\\tat org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:821)\\n\\tat org.apache.lucene.search.IndexSearcher.search(IndexSearcher.java:535)\\n\\tat org.neo4j.index.impl.lucene.explicit.LuceneExplicitIndex.search(LuceneExplicitIndex.java:365)\\n\\tat org.neo4j.index.impl.lucene.explicit.LuceneExplicitIndex.query(LuceneExplicitIndex.java:280)\\n\\tat org.neo4j.index.impl.lucene.explicit.LuceneExplicitIndex.query(LuceneExplicitIndex.java:223)\\n\\tat org.neo4j.index.impl.lucene.explicit.LuceneExplicitIndex.query(LuceneExplicitIndex.java:235)\\n\\tat org.neo4j.kernel.impl.newapi.Read.nodeExplicitIndexQuery(Read.java:484)\\n\\tat org.neo4j.cypher.internal.spi.v2_3.TransactionBoundQueryContext$NodeOperations.indexQuery(TransactionBoundQueryContext.scala:533)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.spi.DelegatingOperations.indexQuery(DelegatingQueryContext.scala:184)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.ExceptionTranslatingQueryContext$ExceptionTranslatingOperations.org$neo4j$cypher$internal$compatibility$v2_3$ExceptionTranslatingQueryContext$ExceptionTranslatingOperations$$super$indexQuery(ExceptionTranslatingQueryContext.scala:185)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.ExceptionTranslatingQueryContext$ExceptionTranslatingOperations$$anonfun$indexQuery$1.apply(ExceptionTranslatingQueryContext.scala:185)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.ExceptionTranslatingQueryContext$ExceptionTranslatingOperations$$anonfun$indexQuery$1.apply(ExceptionTranslatingQueryContext.scala:185)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.ExceptionTranslatingQueryContext.org$neo4j$cypher$internal$compatibility$v2_3$ExceptionTranslatingQueryContext$$translateException(ExceptionTranslatingQueryContext.scala:195)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.ExceptionTranslatingQueryContext$ExceptionTranslatingOperations.indexQuery(ExceptionTranslatingQueryContext.scala:185)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.commands.EntityProducerFactory$$anonfun$2$$anonfun$applyOrElse$2.apply(EntityProducerFactory.scala:76)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.commands.EntityProducerFactory$$anonfun$2$$anonfun$applyOrElse$2.apply(EntityProducerFactory.scala:74)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.commands.EntityProducerFactory$$anon$1.apply(EntityProducerFactory.scala:36)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.commands.EntityProducerFactory$$anon$1.apply(EntityProducerFactory.scala:35)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.StartPipe$$anonfun$internalCreateResults$1.apply(StartPipe.scala:37)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.StartPipe$$anonfun$internalCreateResults$1.apply(StartPipe.scala:36)\\n\\tat scala.collection.Iterator$$anon$12.nextCur(Iterator.scala:435)\\n\\tat scala.collection.Iterator$$anon$12.hasNext(Iterator.scala:441)\\n\\tat scala.collection.Iterator$class.isEmpty(Iterator.scala:331)\\n\\tat scala.collection.AbstractIterator.isEmpty(Iterator.scala:1334)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.LimitPipe.internalCreateResults(LimitPipe.scala:32)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.PipeWithSource.createResults(Pipe.scala:125)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.PipeWithSource.createResults(Pipe.scala:122)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.pipes.PipeWithSource.createResults(Pipe.scala:122)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.executionplan.DefaultExecutionResultBuilderFactory$ExecutionWorkflowBuilder.createResults(DefaultExecutionResultBuilderFactory.scala:93)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.executionplan.DefaultExecutionResultBuilderFactory$ExecutionWorkflowBuilder.build(DefaultExecutionResultBuilderFactory.scala:63)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1.apply(ExecutionPlanBuilder.scala:169)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1.apply(ExecutionPlanBuilder.scala:153)\\n\\tat org.neo4j.cypher.internal.compiler.v2_3.executionplan.ExecutionPlanBuilder$$anon$1.run(ExecutionPlanBuilder.scala:110)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.Compatibility$ExecutionPlanWrapper$$anonfun$run$1.apply(Compatibility.scala:114)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.Compatibility$ExecutionPlanWrapper$$anonfun$run$1.apply(Compatibility.scala:112)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.exceptionHandler$runSafely$.apply(exceptionHandler.scala:85)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.Compatibility$ExecutionPlanWrapper.run(Compatibility.scala:112)\\n\\tat org.neo4j.cypher.internal.compatibility.v2_3.Compatibility$ExecutionPlanWrapper.run(Compatibility.scala:143)\\n\\tat org.neo4j.cypher.internal.PreparedPlanExecution.execute(PreparedPlanExecution.scala:29)\\n\\tat org.neo4j.cypher.internal.ExecutionEngine.execute(ExecutionEngine.scala:119)\\n\\tat org.neo4j.cypher.internal.ExecutionEngine.execute(ExecutionEngine.scala:111)\\n\\tat org.neo4j.cypher.internal.javacompat.ExecutionEngine.executeQuery(ExecutionEngine.java:75)\\n\\tat org.neo4j.server.rest.transactional.TransactionHandle.safelyExecute(TransactionHandle.java:370)\\n\\tat org.neo4j.server.rest.transactional.TransactionHandle.executeStatements(TransactionHandle.java:322)\\n\\tat org.neo4j.server.rest.transactional.TransactionHandle.commit(TransactionHandle.java:156)\\n\\tat org.neo4j.server.rest.web.TransactionalService.lambda$executeStatementsAndCommit$1(TransactionalService.java:204)\\n\\tat com.sun.jersey.core.impl.provider.entity.StreamingOutputProvider.writeTo(StreamingOutputProvider.java:71)\\n\\tat com.sun.jersey.core.impl.provider.entity.StreamingOutputProvider.writeTo(StreamingOutputProvider.java:57)\\n\\tat com.sun.jersey.spi.container.ContainerResponse.write(ContainerResponse.java:302)\\n\\tat com.sun.jersey.server.impl.application.WebApplicationImpl._handleRequest(WebApplicationImpl.java:1510)\\n\\tat com.sun.jersey.server.impl.application.WebApplicationImpl.handleRequest(WebApplicationImpl.java:1419)\\n\\tat com.sun.jersey.server.impl.application.WebApplicationImpl.handleRequest(WebApplicationImpl.java:1409)\\n\\tat com.sun.jersey.spi.container.servlet.WebComponent.service(WebComponent.java:409)\\n\\tat com.sun.jersey.spi.container.servlet.ServletContainer.service(ServletContainer.java:558)\\n\\tat com.sun.jersey.spi.container.servlet.ServletContainer.service(ServletContainer.java:733)\\n\\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:790)\\n\\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:873)\\n\\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1623)\\n\\tat org.neo4j.server.rest.dbms.AuthorizationDisabledFilter.doFilter(AuthorizationDisabledFilter.java:49)\\n\\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1610)\\n\\tat org.neo4j.server.rest.web.CorsFilter.doFilter(CorsFilter.java:115)\\n\\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1610)\\n\\tat org.neo4j.server.rest.web.CollectUserAgentFilter.doFilter(CollectUserAgentFilter.java:69)\\n\\tat org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1610)\\n\\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:540)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:255)\\n\\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1700)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:255)\\n\\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1345)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:203)\\n\\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:480)\\n\\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1667)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:201)\\n\\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1247)\\n\\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:144)\\n\\tat org.eclipse.jetty.server.handler.HandlerList.handle(HandlerList.java:61)\\n\\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:132)\\n\\tat org.eclipse.jetty.server.Server.handle(Server.java:505)\\n\\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:370)\\n\\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:267)\\n\\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:305)\\n\\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103)\\n\\tat org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:117)\\n\\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:333)\\n\\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:310)\\n\\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:168)\\n\\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.run(EatWhatYouKill.java:126)\\n\\tat org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor.java:366)\\n\\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:786)\\n\\tat org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:743)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\"\n" +
                "  } ]\n" +
                "}";
    }

    @Test
    public void nonEmptyResults() throws JsonProcessingException {
        String responseString = getSuccessfulResult();

        boolean nonEmpty = RequestHelper.nonEmptyResults(responseString);

        assertTrue(nonEmpty);
    }

    public static String getSuccessfulResult() {
        return "{\n" +
                "  \"results\" : [ {\n" +
                "    \"columns\" : [ \"study.citation\" ],\n" +
                "    \"data\" : [ {\n" +
                "      \"row\" : [ \"Severe acute respiratory syndrome coronavirus 2 isolate hCoV-19/Netherlands/Gelderland_68/2020 genome assembly, complete genome: monopartite\" ],\n" +
                "      \"meta\" : [ null ]\n" +
                "    } ]\n" +
                "  } ],\n" +
                "  \"errors\" : [ ]\n" +
                "}";
    }

    @Test
    public void getExternalId() {
        assertThat(RequestHelper.getUrlFromExternalId("{ \"data\": [[]]}"), is("{}"));
        assertThat(RequestHelper.getUrlFromExternalId("{ \"data\": []}"), is("{}"));
        assertThat(RequestHelper.getUrlFromExternalId("{}"), is("{}"));
    }


}
