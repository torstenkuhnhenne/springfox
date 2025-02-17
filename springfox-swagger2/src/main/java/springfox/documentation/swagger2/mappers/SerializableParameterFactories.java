/*
 *
 *  Copyright 2015-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.swagger2.mappers;

import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.CookieParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.service.Parameter;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.springframework.util.StringUtils.*;
import static springfox.documentation.swagger2.mappers.EnumMapper.*;
import static springfox.documentation.swagger2.mappers.Properties.*;

public class SerializableParameterFactories {
  private static final Map<String, SerializableParameterFactory> FACTORY_MAP = unmodifiableMap(Stream.of(
      new AbstractMap.SimpleEntry<>("header", new HeaderSerializableParameterFactory()),
      new AbstractMap.SimpleEntry<>("form", new FormSerializableParameterFactory()),
      new AbstractMap.SimpleEntry<>("path", new PathSerializableParameterFactory()),
      new AbstractMap.SimpleEntry<>("query", new QuerySerializableParameterFactory()),
      new AbstractMap.SimpleEntry<>("cookie", new CookieSerializableParameterFactory()))
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

  private static final VendorExtensionsMapper VENDOR_EXTENSIONS_MAPPER = new VendorExtensionsMapper();


  private SerializableParameterFactories() {
    throw new UnsupportedOperationException();
  }

  static Optional<io.swagger.models.parameters.Parameter> create(Parameter source) {
    String safeSourceParamType = ofNullable(source.getParamType()).map(String::toLowerCase).orElse("");
    SerializableParameterFactory factory = SerializableParameterFactories.FACTORY_MAP.getOrDefault(safeSourceParamType,
        new NullSerializableParameterFactory());

    SerializableParameter toReturn = factory.create(source);
    if (toReturn == null) {
      return empty();
    }
    ModelReference paramModel = source.getModelRef();
    toReturn.setName(source.getName());
    toReturn.setDescription(source.getDescription());
    toReturn.setAccess(source.getParamAccess());
    toReturn.setPattern(source.getPattern());
    toReturn.setRequired(source.isRequired());
    toReturn.setAllowEmptyValue(source.isAllowEmptyValue());
    toReturn.getVendorExtensions()
        .putAll(VENDOR_EXTENSIONS_MAPPER.mapExtensions(source.getVendorExtentions()));
    Property property = property(paramModel.getType());
    maybeAddAllowableValuesToParameter(toReturn, property, source.getAllowableValues());
    if (paramModel.isCollection()) {
      if (paramModel.getItemType().equals("byte")) {
        toReturn.setType("string");
        toReturn.setFormat("byte");
      } else {
        toReturn.setCollectionFormat(collectionFormat(source));
        toReturn.setType("array");
        ModelReference paramItemModelRef = paramModel.itemModel().get();
        Property itemProperty
            = maybeAddAllowableValues(
            itemTypeProperty(paramItemModelRef),
            source.getAllowableValues());
        toReturn.setItems(itemProperty);
        maybeAddAllowableValuesToParameter(toReturn, itemProperty, paramItemModelRef.getAllowableValues());
      }
    } else if (paramModel.isMap()) {
      ModelReference paramItemModelRef = paramModel.itemModel().get();
      Property itemProperty = new MapProperty(itemTypeProperty(paramItemModelRef));
      toReturn.setItems(itemProperty);
    } else {
      //TODO: swagger-core remove this downcast when swagger-core fixes its problem
      ((AbstractSerializableParameter) toReturn).setDefaultValue(source.getDefaultValue());
      if (source.getScalarExample() != null) {
        ((AbstractSerializableParameter) toReturn).setExample(String.valueOf(source.getScalarExample()));
      }
      toReturn.setType(property.getType());
      toReturn.setFormat(property.getFormat());
    }
    return of(toReturn);
  }

  private static String collectionFormat(Parameter source) {
    return isEmpty(source.getCollectionFormat()) ? "multi" : source.getCollectionFormat();
  }

  static class CookieSerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      CookieParameter param = new CookieParameter();
      param.setDefaultValue(source.getDefaultValue());
      return param;
    }
  }

  static class FormSerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      FormParameter param = new FormParameter();
      param.setDefaultValue(source.getDefaultValue());
      return param;
    }
  }

  static class HeaderSerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      HeaderParameter param = new HeaderParameter();
      param.setDefaultValue(source.getDefaultValue());
      return param;
    }
  }

  static class PathSerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      PathParameter param = new PathParameter();
      param.setDefaultValue(source.getDefaultValue());
      return param;
    }
  }

  static class QuerySerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      QueryParameter param = new QueryParameter();
      param.setDefaultValue(source.getDefaultValue());
      return param;
    }
  }

  static class NullSerializableParameterFactory implements SerializableParameterFactory {
    @Override
    public SerializableParameter create(Parameter source) {
      return null;
    }
  }
}
