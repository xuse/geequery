package jef.database.meta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.database.annotation.DynamicTable;
import jef.database.query.Query;
import jef.tools.reflect.Property;

public class ExtensionTemplate implements ExtensionFactory{
	
	private final Map<String,ExtensionInstance> cache=new ConcurrentHashMap<String, ExtensionInstance>(); 
	private DynamicTable dt;
	private Property keyAccessor;
	
	public ExtensionTemplate(DynamicTable dt){
		this.dt=dt;
	}

	@Override
	public ExtensionConfig valueOf(Query<?> q) {
		if(q==null){
			throw new IllegalArgumentException();
		}
		String key=(String)keyAccessor.get(q.getInstance());
		if(key==null){
			throw new IllegalArgumentException();
		}
		ExtensionInstance ec=cache.get(key);
		if(ec!=null)return ec;
		ec=new ExtensionInstance(key);
		cache.put(key,ec);
		return ec;
	}
	
	class ExtensionInstance extends AbstractExtensionConfig{
		public ExtensionInstance(String key) {
			this.name=key;
		}

	}
}
