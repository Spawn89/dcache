/**
 * ArrayOfTRequestTokenReturn.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.dcache.srm.v2_2;

public class ArrayOfTRequestTokenReturn  implements java.io.Serializable {
    private static final long serialVersionUID = 7446982766436796463L;
    private org.dcache.srm.v2_2.TRequestTokenReturn[] tokenArray;

    public ArrayOfTRequestTokenReturn() {
    }

    public ArrayOfTRequestTokenReturn(
           org.dcache.srm.v2_2.TRequestTokenReturn[] tokenArray) {
           this.tokenArray = tokenArray;
    }


    /**
     * Gets the tokenArray value for this ArrayOfTRequestTokenReturn.
     * 
     * @return tokenArray
     */
    public org.dcache.srm.v2_2.TRequestTokenReturn[] getTokenArray() {
        return tokenArray;
    }


    /**
     * Sets the tokenArray value for this ArrayOfTRequestTokenReturn.
     * 
     * @param tokenArray
     */
    public void setTokenArray(org.dcache.srm.v2_2.TRequestTokenReturn[] tokenArray) {
        this.tokenArray = tokenArray;
    }

    public org.dcache.srm.v2_2.TRequestTokenReturn getTokenArray(int i) {
        return this.tokenArray[i];
    }

    public void setTokenArray(int i, org.dcache.srm.v2_2.TRequestTokenReturn _value) {
        this.tokenArray[i] = _value;
    }

    private java.lang.Object __equalsCalc;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ArrayOfTRequestTokenReturn)) {
            return false;
        }
        ArrayOfTRequestTokenReturn other = (ArrayOfTRequestTokenReturn) obj;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.tokenArray==null && other.getTokenArray()==null) || 
             (this.tokenArray!=null &&
              java.util.Arrays.equals(this.tokenArray, other.getTokenArray())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getTokenArray() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getTokenArray());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getTokenArray(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ArrayOfTRequestTokenReturn.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://srm.lbl.gov/StorageResourceManager", "ArrayOfTRequestTokenReturn"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("tokenArray");
        elemField.setXmlName(new javax.xml.namespace.QName("", "tokenArray"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://srm.lbl.gov/StorageResourceManager", "TRequestTokenReturn"));
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
