package com.querydsl.sql.codegen;

import com.querydsl.codegen.EntityType;

public class HoneggerNamingStrategy extends DefaultNamingStrategy {

	@Override
	public String getPropertyNameForPrimaryKey(String pkName, EntityType entityType) {
		return "PK";
		// if (pkName.toLowerCase().startsWith("pk_")) {
		// 	pkName = pkName.substring(3) + "_" + pkName.substring(0,2);
		// }
		// String propertyName = getPropertyName(pkName, entityType);
		// for (String candidate : ImmutableList.of(propertyName, propertyName + "Pk")) {
		// 	if (!entityType.getEscapedPropertyNames().contains(candidate)) {
		// 		return normalizeJavaName(candidate);
		// 	}
		// }
		// return normalizeJavaName(escape(entityType, propertyName));
	}
}
