package org.neo4j.gis.spatial;

import org.geotools.data.ResourceInfo;
import org.geotools.data.neo4j.Neo4jSpatialDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.gis.spatial.osm.OSMImporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class Neo4jSpatialDataStoreTest {

    private DatabaseManagementService databases;
    public GraphDatabaseService graph;

    @Before
    public void setup() throws Exception {
        this.databases = new TestDatabaseManagementServiceBuilder(Path.of("target", "test")).impermanent().build();
        this.graph = databases.database(DEFAULT_DATABASE_NAME);
        OSMImporter importer = new OSMImporter("map", new ConsoleListener());
        importer.setCharset(StandardCharsets.UTF_8);
        importer.setVerbose(false);
        importer.importFile(graph, "map.osm");
        importer.reIndex(graph);
    }

    @After
    public void teardown() {
        if (this.databases != null) {
            this.databases.shutdown();
            this.databases = null;
            this.graph = null;
        }
    }

    @Test
    public void shouldOpenDataStore() {
        Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(graph);
        ReferencedEnvelope bounds = store.getBounds("map");
        assertThat(bounds, equalTo(new ReferencedEnvelope(12.7856667, 13.2873561, 55.9254241, 56.2179056, DefaultGeographicCRS.WGS84)));
    }

    @Test
    public void shouldOpenDataStoreOnNonSpatialDatabase() {
        DatabaseManagementService otherDatabases = null;
        try {
            otherDatabases = new TestDatabaseManagementServiceBuilder(Path.of("target", "other-db")).impermanent().build();
            GraphDatabaseService otherGraph = otherDatabases.database(DEFAULT_DATABASE_NAME);
            Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(otherGraph);
            ReferencedEnvelope bounds = store.getBounds("map");
            // TODO: rather should throw a descriptive exception
            assertThat(bounds, equalTo(null));
        } finally {
            if (otherDatabases != null)
                otherDatabases.shutdown();
        }
    }

    @Test
    public void shouldBeAbleToListLayers() throws IOException {
        Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(graph);
        String[] layers = store.getTypeNames();
        assertThat("Expected one layer", layers.length, equalTo(1));
        assertThat(layers[0], equalTo("map"));
    }

    @Test
    public void shouldBeAbleToGetSchemaForLayer() throws IOException {
        Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(graph);
        SimpleFeatureType schema = store.getSchema("map");
        assertThat("Expected 25 attributes", schema.getAttributeCount(), equalTo(25));
        assertThat("Expected geometry attribute to be called 'the_geom'", schema.getAttributeDescriptors().get(0).getLocalName(), equalTo("the_geom"));
    }

    @Test
    public void shouldBeAbleToGetFeatureSourceForLayer() throws IOException {
        Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(graph);
        SimpleFeatureSource source = store.getFeatureSource("map");
        SimpleFeatureCollection features = source.getFeatures();
        assertThat("Expected 217 features", features.size(), equalTo(217));
        assertThat("Expected there to be a feature with name 'Nybrodalsvägen'", featureNames(features), hasItem("Nybrodalsvägen"));
    }

    @Test
    public void shouldBeAbleToGetInfoForLayer() throws IOException {
        Neo4jSpatialDataStore store = new Neo4jSpatialDataStore(graph);
        SimpleFeatureSource source = store.getFeatureSource("map");
        ResourceInfo info = source.getInfo();
        ReferencedEnvelope bounds = info.getBounds();
        assertThat(bounds, equalTo(new ReferencedEnvelope(12.7856667, 13.2873561, 55.9254241, 56.2179056, DefaultGeographicCRS.WGS84)));
        SimpleFeatureCollection features = source.getFeatures();
        assertThat("Expected 217 features", features.size(), equalTo(217));
        assertThat("Expected there to be a feature with name 'Nybrodalsvägen'", featureNames(features), hasItem("Nybrodalsvägen"));
    }

    private Set<String> featureNames(SimpleFeatureCollection features) {
        HashSet<String> names = new HashSet<>();
        SimpleFeatureIterator featureIterator = features.features();
        while (featureIterator.hasNext()) {
            SimpleFeature feature = featureIterator.next();
            Object name = feature.getAttribute("name");
            if (name != null) names.add(name.toString());
        }
        return names;
    }
}
