package org.open4goods.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.Rating;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedAttributes  {
	
	/**
	 * The referentiel attributes
	 */
	@Field(index = true, store = false, type = FieldType.Object)
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	@Field(index = true, store = false, type = FieldType.Object)
	//TODO: rename
	private Map<String,AggregatedAttribute> aggregatedAttributes = new HashMap<>();
	

	@Field(index = true, store = false, type = FieldType.Object)
	private Set<AggregatedAttribute> unmapedAttributes = new HashSet<>();

	
	@Field(index = false, store = false, type = FieldType.Object)
	private Set<AggregatedFeature> features = new HashSet<>();

	
	

	
	@Override
	public String toString() {
		return "ref-attrs:"+referentielAttributes.size()+ " , attrs:"+aggregatedAttributes.size() ;
	}
	public void addReferentielAttribute(ReferentielKey key, String value) {
		referentielAttributes.put(key, value);
		
	}
	



	
	
	
	public Map<String, AggregatedAttribute> getAggregatedAttributes() {
		return aggregatedAttributes;
	}
	public void setAggregatedAttributes(Map<String, AggregatedAttribute> aggregatedAttributes) {
		this.aggregatedAttributes = aggregatedAttributes;
	}
	public Set<AggregatedFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<AggregatedFeature> features) {
		this.features = features;
	}

	public Set<AggregatedAttribute> getUnmapedAttributes() {
		return unmapedAttributes;
	}

	public void setUnmapedAttributes(Set<AggregatedAttribute> unmapedAttributes) {
		this.unmapedAttributes = unmapedAttributes;
	}

	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}



	
	
	
	
	

}
