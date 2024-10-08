package ambit2.base.data.study;

import java.util.Map;

import net.idea.modbcum.i.JSONSerializable;

public interface IParams<VALUE> extends Map<String,VALUE>, JSONSerializable {
	public VALUE getUnits();
	public void setUnits(VALUE unit);
	
	public VALUE getLoValue();
	public VALUE getUpValue();
	public void setLoValue(VALUE value);
	public void setUpValue(VALUE value);
	public VALUE getUpQualifier();
	public VALUE getLoQualifier();
	public void setUpQualifier(VALUE qualifier);
	public void setLoQualifier(VALUE qualifier);
}
