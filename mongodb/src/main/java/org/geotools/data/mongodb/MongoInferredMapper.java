package org.geotools.data.mongodb;


import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

/**
 * 
 * @author tkunicki@boundlessgeo.com
 *
 */
public class MongoInferredMapper extends AbstractCollectionMapper {
    
    public final static Logger LOG = Logging.getLogger(MongoInferredMapper.class);

    MongoGeometryBuilder geomBuilder = new MongoGeometryBuilder();

    SimpleFeatureType schema;
    
    @Override
    public String getGeometryPath() {
        String gdName = schema.getGeometryDescriptor().getLocalName();
        return (String)schema.getDescriptor(gdName).getUserData().get(MongoDataStore.KEY_mapping);
    }

    @Override
    public String getPropertyPath(String property) {
        AttributeDescriptor descriptor = schema.getDescriptor(property);
        return descriptor == null ? null :
                (String)descriptor.getUserData().get(MongoDataStore.KEY_mapping);
    }

    @Override
    public Geometry getGeometry(DBObject dbo) {
        Object o = MongoUtil.getDBOValue(dbo, getGeometryPath());
        // TODO legacy coordinate pair
        return o == null ? null : geomBuilder.toGeometry((DBObject)o);
    }

    @Override
    public DBObject toObject(Geometry g) {
        return geomBuilder.toObject(g);
    }

    @Override
    public void setGeometry(DBObject dbo, Geometry g) {
        MongoUtil.setDBOValue(dbo, getGeometryPath(), toObject(g));
    }

    @Override
    public SimpleFeatureType buildFeatureType(Name name, DBCollection collection) {
        
        Set<String> indexedGeometries = MongoUtil.findIndexedGeometries(collection);
        Set<String> indexedFields = MongoUtil.findIndexedFields(collection);
        Map<String, Class<?>> mappedFields = MongoUtil.findMappableFields(collection);
        
        // don't need to worry about indexed properties we've found in our scan...
        indexedFields.removeAll(mappedFields.keySet());
        
        // remove geometries from indexed and mapped sets
        indexedFields.removeAll(indexedGeometries);
        for (String mappedProperty : new ArrayList<String>(mappedFields.keySet())) {
            for (String indexedGeometry : indexedGeometries) {
                if (mappedProperty.startsWith(indexedGeometry)) {
                    mappedFields.remove(mappedProperty);
                    break;
                }
            }
        }
        
        SimpleFeatureTypeBuilder ftBuilder = new SimpleFeatureTypeBuilder();
        ftBuilder.setName(name);
        
        // NOTE: for now we just use first (hopefully only) indexed geometry we find
        String geometryField = indexedGeometries.iterator().next();
        if (indexedGeometries.size() > 1) {
            LOG.log(Level.WARNING, "More than one indexed geometry field found for type {0}, selecting {1} (first one encountered with index search of collection {2})",
                    new Object[] {name, geometryField, collection.getFullName() });
        }
        ftBuilder.userData(MongoDataStore.KEY_mapping, geometryField);
        ftBuilder.userData(MongoDataStore.KEY_encoding, "GeoJSON");
        ftBuilder.add(geometryField, Geometry.class, DefaultGeographicCRS.WGS84);
        LOG.log(Level.INFO, "building type {0}: mapping geometry field {1} from collection {2}",
                    new Object[] {name, geometryField, collection.getFullName() });
        
        for (Map.Entry<String, Class<?>> mappedField : mappedFields.entrySet()) {
            String field = mappedField.getKey();
            Class<?> binding = mappedField.getValue();
            ftBuilder.userData(MongoDataStore.KEY_mapping, field);
            ftBuilder.add(field, binding);
            LOG.log(Level.INFO, "building type \"{0}\": mapping field \"{1}\" with binding {2} from collection {3}",
                    new Object[] {name, field, binding.getName(), collection.getFullName() });
        }
        
        for (String field : indexedFields) {
            ftBuilder.userData(MongoDataStore.KEY_mapping, field);
            ftBuilder.add(field, String.class);
            LOG.log(Level.INFO, "building type \"{0}\": mapping indexed field \"{1}\" with default binding, {2}, from collection {3}",
                    new Object[] {name, field, String.class.getName(), collection.getFullName() });
        }
        
        SimpleFeatureType featureType = ftBuilder.buildFeatureType();
        featureType.getUserData().put(MongoDataStore.KEY_collection, collection.getName());
        
        this.schema = featureType;
        
        return featureType;
    }
}
