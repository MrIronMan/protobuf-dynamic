/*
 * Copyright 2015 protobuf-dynamic developers
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.os72.protobuf.dynamic;

import com.github.os72.protobuf.dynamic.PersonSchema2.Person.Address;
import com.google.protobuf.DynamicMessage.Builder;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.junit.Assert;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;

public class DynamicSchemaTest
{
	/**
	 * testBasic - basic usage
	 */
	@Test
	public void testBasic() throws Exception {
		log("--- testBasic ---");

		// Create dynamic schema
		DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
		schemaBuilder.setName("PersonSchemaDynamic.proto");

		MessageDefinition msgDef = MessageDefinition.newBuilder("Person") // message Person
				.addField("required", "int32", "id", 1)		// required int32 id = 1
				.addField("required", "string", "name", 2)	// required string name = 2
				.addField("optional", "string", "email", 3)	// optional string email = 3
				.build();

		schemaBuilder.addMessageDefinition(msgDef);
		DynamicSchema schema = schemaBuilder.build();
		log(schema);

		/**
		 * 1.方法名，采用反射生成 msgDef
		 * 2.根据传入的值来构建 msgBuilder，生成 byte 数据，存储只 redis
		 *
		 * 1.将 byte 转换为对象 DynamicMessage message = msgBuilder2.mergeFrom(msg.toByteArray()).build();
		 * 2.获取字段：msgDesc.findFieldByName("id").toProto().getName()
		 * 3.使用 jsonObject 转换为对应的对象
		 *
		 * 存在的问题：
		 * 1.Map 或者 list 类型
		 * 2.时间转换问题
		 * 3.嵌套对象
		 */

		// Create dynamic message from schema
		DynamicMessage.Builder msgBuilder = schema.newMessageBuilder("Person");
		Descriptor msgDesc = msgBuilder.getDescriptorForType();
		DynamicMessage msg = msgBuilder
				.setField(msgDesc.findFieldByName("id"), 1)
				.setField(msgDesc.findFieldByName("name"), "Alan Turing")
				.setField(msgDesc.findFieldByName("email"), "at@sis.gov.uk")
				.build();
		log(msg);

		// Create data object traditional way using generated code
		PersonSchema2.Person person = PersonSchema2.Person.newBuilder()
				.setId(1)
				.setName("Alan Turing")
				.setEmail("at@sis.gov.uk")
				.build();

		// model to byte && byte to model
		DynamicMessage.Builder msgBuilder2 = schema.newMessageBuilder("Person");
		DynamicMessage message = msgBuilder2.mergeFrom(person.toByteArray()).build();

		JSON.toJSONString(person);
		String data = JsonFormat.printer().print(msg);
		DynamicMessage.Builder msgBuilder3 = schema.newMessageBuilder("Person");
		JsonFormat.parser().merge(data, msgBuilder3);
		DynamicMessage dynamicMessage = msgBuilder3.build();

		// Should be equivalent
		Assert.assertEquals(person.toString(), msg.toString());
	}

	@Test
	public void testGenerate() throws Exception{
		// Create dynamic schema
		DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
		schemaBuilder.setName("PersonSchemaDynamicTest.proto");

		MessageDefinition mapDef = MessageDefinition.newBuilder("Map")
			.addField("required", "string", "key", 1)
			.addField("required", "string", "value", 2)
			.build();

		MessageDefinition addressMsgDef = MessageDefinition.newBuilder("Address")
			.addField("optional", "string", "street", 1)
			.addField("required", "int32", "num", 2)
			.build();

		MessageDefinition msgDef = MessageDefinition.newBuilder("Person") // message Person
			.addField("required", "int32", "id", 1)		// required int32 id = 1
			.addField("required", "string", "name", 2)	// required string name = 2
			.addField("optional", "string", "email", 3)	// optional string email = 3
			.addField("optional", "Address", "add", 4)
			.addField("optional", "Map", "maps", 5)
			.build();

		schemaBuilder.addMessageDefinition(msgDef);
		schemaBuilder.addMessageDefinition(addressMsgDef);
		schemaBuilder.addMessageDefinition(mapDef);
		DynamicSchema schema = schemaBuilder.build();
		log(schema);

		// Create dynamic message from schema
		DynamicMessage.Builder msgBuilder = schema.newMessageBuilder("Person");
		Builder addressBuilder = schema.newMessageBuilder("Address");
		Descriptor addressDesc = addressBuilder.getDescriptorForType();
		Descriptor msgDesc = msgBuilder.getDescriptorForType();
		Builder mapBuilder = schema.newMessageBuilder("Map");
		Descriptor mapDescriptor = mapBuilder.getDescriptorForType();
		DynamicMessage addressMessage = addressBuilder
			.setField(addressDesc.findFieldByName("street"), "asd")
			.setField(addressDesc.findFieldByName("num"), 12)
			.build();
		DynamicMessage mapMsg = mapBuilder
			.setField(mapDescriptor.findFieldByName("key"), "str")
			.setField(mapDescriptor.findFieldByName("value"), "str")
			.build();
		Map<String, String> map = new HashMap<>();
		map.put("str", "str");
		DynamicMessage msg = msgBuilder
			.setField(msgDesc.findFieldByName("id"), 1)
			.setField(msgDesc.findFieldByName("name"), "Alan Turing")
			.setField(msgDesc.findFieldByName("email"), "at@sis.gov.uk")
			.setField(msgDesc.findFieldByName("add"), addressMessage)
			.setField(msgDesc.findFieldByName("maps"), mapMsg)
			.build();
		log(msg);

		// Create data object traditional way using generated code
		PersonSchema2.Person person = PersonSchema2.Person.newBuilder()
			.setId(1)
			.setName("Alan Turing")
			.setEmail("at@sis.gov.uk")
			.setAdd(Address.newBuilder()
				.setStreet("asd")
				.setNum(12)
				.build())
			.putStrMap("str", "str")
			.build();
		List<PersonSchema2.Person> list = new ArrayList<>();

		// Should be equivalent
		Assert.assertEquals(person.toString(), msg.toString());

		byte[] msgBytes = msg.toByteArray();
		byte[] modelBytes = person.toByteArray();
		DynamicMessage generateMsg = msgBuilder.mergeFrom(msgBytes).build();

		String data = JsonFormat.printer().print(generateMsg);
		System.out.println("alibaba.fastjson:" + JSON.toJSONString(person));
		System.out.println("protocol.json:" + data);
		DynamicMessage.Builder msgBuilder3 = schema.newMessageBuilder("Person");
		JsonFormat.parser().merge(data, msgBuilder3);
		Person realData = JSON.parseObject(data, Person.class);
		DynamicMessage dynamicMessage = msgBuilder3.build();
		Assert.assertEquals(person.toString(), dynamicMessage.toString());
	}

	@Test
	public void testComplex() throws Exception {
		log("--- testBasic ---");
		Descriptors.FieldDescriptor strMap1 = PersonSchema2.Person.newBuilder().build().getDescriptorForType().findFieldByName("strMap");

		// Create dynamic schema
		DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
		schemaBuilder.setName("PersonSchemaDynamic.proto");

		EnumDefinition enumDefPhoneType = EnumDefinition.newBuilder("PhoneType") // enum PhoneType
				.addValue("MOBILE", 0)	// MOBILE = 0
				.addValue("HOME", 1)	// HOME = 1
				.addValue("WORK", 2)	// WORK = 2
				.build();

		MessageDefinition msgDefPhoneNumber = MessageDefinition.newBuilder("PhoneNumber") // message PhoneNumber
				.addField("required", "string", "number", 1)			// required string number = 1
				.addField("optional", "PhoneType", "type", 2, "HOME")	// optional PhoneType type = 2 [default = HOME]
				.build();

		MessageDefinition mapDef = MessageDefinition.newBuilder("Map")
				.addField("optional", "string", "key", 1)
				.addField("optional", "string", "value", 2)
				.build();

		MessageDefinition phoneMapDef = MessageDefinition.newBuilder("PhoneMap")
				.addField("optional", "string", "key", 1)
				.addField("optional", "PhoneNumber", "value", 2)
				.build();

		MessageDefinition strMapDef = MessageDefinition.newBuilder("StrMapEntry")
				.addField(strMap1.toProto())
				.build();

//		strMap1.toProto()


		MessageDefinition msgDef = MessageDefinition.newBuilder("Person") // message Person
				.addEnumDefinition(enumDefPhoneType)
				.addMessageDefinition(msgDefPhoneNumber)
				.addMessageDefinition(mapDef)
				.addMessageDefinition(phoneMapDef)
				.addMessageDefinition(strMapDef)
				.addField("required", "int32", "id", 1)		// required int32 id = 1
				.addField("required", "string", "name", 2)	// required string name = 2
				.addField("optional", "string", "email", 3)	// optional string email = 3
				.addField("repeated", "PhoneNumber", "phone", 4)	// repeated PhoneNumber phone = 4
				.addField("optional", "message", "strMap", 5) // optional map = 4
//				.addField(strMap1.toProto())
//				.addField("optional", "message", "phoneMap", 6) // optional map = 5
				.build();

//		.Person.StrMapEntry
		schemaBuilder.addMessageDefinition(msgDef);
		DynamicSchema schema = schemaBuilder.build();
		log(schema);


		DynamicMessage.Builder messageBuilder = schema.newMessageBuilder("Person");
		Descriptor descriptor = messageBuilder.getDescriptorForType();

		Descriptor phoneDesc = schema.getMessageDescriptor("Person.PhoneNumber");
		DynamicMessage phoneMsg1 = schema.newMessageBuilder("Person.PhoneNumber")
				.setField(phoneDesc.findFieldByName("number"), "+44-111")
				.build();
		DynamicMessage phoneMsg2 = schema.newMessageBuilder("Person.PhoneNumber")
				.setField(phoneDesc.findFieldByName("number"), "+44-222")
				.setField(phoneDesc.findFieldByName("type"), schema.getEnumValue("Person.PhoneType", "WORK"))
				.build();
		Descriptor personDesc = schema.getMessageDescriptor("Person");
		Map<String, String> strMap = new HashMap<String, String>();
		strMap.put("aaa", "aaa");
		strMap.put("bbb", "bbb");

		Map<Long, DynamicMessage> phoneMap = new HashMap<Long, DynamicMessage>();
		phoneMap.put(1L, phoneMsg1);
		phoneMap.put(2L, phoneMsg2);

		DynamicMessage msg = messageBuilder
				.setField(descriptor.findFieldByName("id"), 1)
				.setField(descriptor.findFieldByName("name"), "Alan Turing")
				.setField(descriptor.findFieldByName("email"), "at@sis.gov.uk")
				.addRepeatedField(personDesc.findFieldByName("phone"), phoneMsg1)
				.addRepeatedField(personDesc.findFieldByName("phone"), phoneMsg2)
				.setField(descriptor.findFieldByName("strMap"), strMap)
				.setField(descriptor.findFieldByName("phoneMap"), phoneMap)
				.build();

		log(msg);


		// Create data object traditional way using generated code
		Map<Long, PersonSchema2.Person.PhoneNumber> phoneNumberHashMap = new HashMap<Long, PersonSchema2.Person.PhoneNumber>();
		phoneNumberHashMap.put(1L, PersonSchema2.Person.PhoneNumber.newBuilder()
				.setType(PersonSchema2.Person.PhoneType.WORK)
				.setNumber("+44-222")
				.build());
		phoneNumberHashMap.put(2L, PersonSchema2.Person.PhoneNumber.newBuilder()
				.setNumber("+44-111")
				.buildPartial());
		phoneMap.put(2L, phoneMsg2);
		PersonSchema2.Person person = PersonSchema2.Person.newBuilder()
				.setId(1)
				.setName("Alan Turing")
				.setEmail("at@sis.gov.uk")
				.addPhone(PersonSchema2.Person.PhoneNumber.newBuilder()
						.setType(PersonSchema2.Person.PhoneType.WORK)
						.setNumber("+44-222")
						.build())
				.addPhone(PersonSchema2.Person.PhoneNumber.newBuilder()
						.setNumber("+44-111")
						.buildPartial())
				.putAllStrMap(strMap)
				.putAllPhoneMap(phoneNumberHashMap)
				.build();
		log(person);

		Assert.assertEquals(person.toString(), msg.toString());
	}

	/**
	 * testOneof - oneof usage
	 */
	@Test
	public void testOneof() throws Exception {
		log("--- testOneof ---");
		
		// Create dynamic schema
		DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
		schemaBuilder.setName("PersonSchemaDynamic.proto");
		
		MessageDefinition msgDef = MessageDefinition.newBuilder("Person") // message Person
				.addField("required", "int32", "id", 1)		// required int32 id = 1
				.addField("required", "string", "name", 2)	// required string name = 2
				.addField("optional", "string", "email", 3)	// optional string email = 3
				.addOneof("address")						// oneof address
					.addField("string", "home_addr", 4)		// string home_addr = 4
					.addField("string", "work_addr", 5)		// string work_addr = 5
					.msgDefBuilder()
				.build();
		
		// Demo OneofBuilder
		MessageDefinition.Builder msgDefBuilder = MessageDefinition.newBuilder("SomeOneofDef");
		msgDefBuilder.addField("required", "int32", "id", 1);
		MessageDefinition.OneofBuilder oneofBuilder1 = msgDefBuilder.addOneof("addr1");
		MessageDefinition.OneofBuilder oneofBuilder2 = msgDefBuilder.addOneof("addr2");
		oneofBuilder1.addField("string", "addr11", 11);
		oneofBuilder1.addField("string", "addr12", 12);
		oneofBuilder2.addField("string", "addr21", 21, "default21");
		oneofBuilder2.addField("string", "addr22", 22, "default22");
		schemaBuilder.addMessageDefinition(msgDefBuilder.build());
		
		schemaBuilder.addMessageDefinition(msgDef);
		DynamicSchema schema = schemaBuilder.build();
		log(schema);
		
		// Create dynamic message from schema
		DynamicMessage.Builder msgBuilder = schema.newMessageBuilder("Person");
		Descriptor msgDesc = msgBuilder.getDescriptorForType();
		DynamicMessage msg = msgBuilder
				.setField(msgDesc.findFieldByName("id"), 1)
				.setField(msgDesc.findFieldByName("name"), "Alan Turing")
                .setField(msgDesc.findFieldByName("work_addr"), "85 Albert Embankment")
				.build();
		log(msg);
		
		// Create data object traditional way using generated code 
		PersonSchema.Person person = PersonSchema.Person.newBuilder()
				.setId(1)
				.setName("Alan Turing")
				.setWorkAddr("85 Albert Embankment")
				.build();
		
		// Should be equivalent
		Assert.assertEquals(person.toString(), msg.toString());
	}

	/**
	 * testAdvanced - nested messages, enums, default values, repeated fields
	 */
	@Test
	public void testAdvanced() throws Exception {
		log("--- testAdvanced ---");
		
		// Create dynamic schema
		DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
		schemaBuilder.setName("PersonSchemaDynamic.proto");
		
		EnumDefinition enumDefPhoneType = EnumDefinition.newBuilder("PhoneType") // enum PhoneType
				.addValue("MOBILE", 0)	// MOBILE = 0
				.addValue("HOME", 1)	// HOME = 1
				.addValue("WORK", 2)	// WORK = 2
				.build();
		
		MessageDefinition msgDefPhoneNumber = MessageDefinition.newBuilder("PhoneNumber") // message PhoneNumber
				.addField("required", "string", "number", 1)			// required string number = 1
				.addField("optional", "PhoneType", "type", 2, "HOME")	// optional PhoneType type = 2 [default = HOME]
				.build();
		
		MessageDefinition msgDefPerson = MessageDefinition.newBuilder("Person") // message Person
				.addEnumDefinition(enumDefPhoneType)				// enum PhoneType (nested)
				.addMessageDefinition(msgDefPhoneNumber)			// message PhoneNumber (nested)
				.addField("required", "int32", "id", 1)				// required int32 id = 1
				.addField("required", "string", "name", 2)			// required string name = 2
				.addField("optional", "string", "email", 3)			// optional string email = 3
				.addField("repeated", "PhoneNumber", "phone", 4)	// repeated PhoneNumber phone = 4
				.build();
		
		schemaBuilder.addMessageDefinition(msgDefPerson);
		DynamicSchema schema = schemaBuilder.build();
		log(schema);
		
		// Create dynamic message from schema
		Descriptor phoneDesc = schema.getMessageDescriptor("Person.PhoneNumber");
		DynamicMessage phoneMsg1 = schema.newMessageBuilder("Person.PhoneNumber")
				.setField(phoneDesc.findFieldByName("number"), "+44-111")
				.build();
		DynamicMessage phoneMsg2 = schema.newMessageBuilder("Person.PhoneNumber")
				.setField(phoneDesc.findFieldByName("number"), "+44-222")
				.setField(phoneDesc.findFieldByName("type"), schema.getEnumValue("Person.PhoneType", "WORK"))
				.build();
		
		Descriptor personDesc = schema.getMessageDescriptor("Person");
		DynamicMessage personMsg = schema.newMessageBuilder("Person")
				.setField(personDesc.findFieldByName("id"), 1)
				.setField(personDesc.findFieldByName("name"), "Alan Turing")
				.setField(personDesc.findFieldByName("email"), "at@sis.gov.uk")
				.addRepeatedField(personDesc.findFieldByName("phone"), phoneMsg1)
				.addRepeatedField(personDesc.findFieldByName("phone"), phoneMsg2)
				.build();

		// Create data object traditional way using generated code
		PersonSchema.Person person = PersonSchema.Person.newBuilder()
				.setId(1)
				.setName("Alan Turing")
				.setEmail("at@sis.gov.uk")
				.addPhone(PersonSchema.Person.PhoneNumber.newBuilder()
						.setNumber("+44-111")
						.build())
				.addPhone(PersonSchema.Person.PhoneNumber.newBuilder()
						.setNumber("+44-222")
						.setType(PersonSchema.Person.PhoneType.WORK)
						.build())
				.build();

		log(person);
		log(personMsg);
		Assert.assertEquals(person.toString(), personMsg.toString());
		log("=============");
		
		phoneMsg1 = (DynamicMessage)personMsg.getRepeatedField(personDesc.findFieldByName("phone"), 0);
		phoneMsg2 = (DynamicMessage)personMsg.getRepeatedField(personDesc.findFieldByName("phone"), 1);
		
		String phoneNumber1 = (String)phoneMsg1.getField(phoneDesc.findFieldByName("number"));		
		String phoneNumber2 = (String)phoneMsg2.getField(phoneDesc.findFieldByName("number"));
		
		EnumValueDescriptor phoneType1 = (EnumValueDescriptor)phoneMsg1.getField(phoneDesc.findFieldByName("type"));
		EnumValueDescriptor phoneType2 = (EnumValueDescriptor)phoneMsg2.getField(phoneDesc.findFieldByName("type"));
		
		log(phoneNumber1 + ", " + phoneType1.getName());
		log(phoneNumber2 + ", " + phoneType2.getName());
		
		Assert.assertEquals("+44-111", phoneNumber1);
		Assert.assertEquals("HOME", phoneType1.getName()); // [default = HOME]
		
		Assert.assertEquals("+44-222", phoneNumber2);
		Assert.assertEquals("WORK", phoneType2.getName());
	}

	/**
	 * testSchemaMerge - schema merging
	 */
	@Test
	public void testSchemaMerge() throws Exception {
		log("--- testSchemaMerge ---");
		
		DynamicSchema.Builder schemaBuilder1 = DynamicSchema.newBuilder().setName("Schema1.proto").setPackage("package1");
		schemaBuilder1.addMessageDefinition(MessageDefinition.newBuilder("Msg1").build());
		
		DynamicSchema.Builder schemaBuilder2 = DynamicSchema.newBuilder().setName("Schema2.proto").setPackage("package2");
		schemaBuilder2.addMessageDefinition(MessageDefinition.newBuilder("Msg2").build());
		
		schemaBuilder1.addSchema(schemaBuilder2.build());
		DynamicSchema schema1 = schemaBuilder1.build(); 
		log(schema1);
		
		// schema1 should contain both Msg1 and Msg2
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg1"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg2"));
		
		DynamicSchema.Builder schemaBuilder3 = DynamicSchema.newBuilder().setName("Schema3.proto").setPackage("package3");
		schemaBuilder3.addMessageDefinition(MessageDefinition.newBuilder("Msg1").build()); // Msg1 to force collision
		schemaBuilder1.addSchema(schemaBuilder3.build());
		schema1 = schemaBuilder1.build(); 
		log(schema1);
		
		// Msg1 now ambiguous, must fully qualify name (package1, package3); Msg2 still unique
		Assert.assertNull(schema1.getMessageDescriptor("Msg1"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg2"));
		Assert.assertNotNull(schema1.getMessageDescriptor("package1.Msg1"));
		Assert.assertNotNull(schema1.getMessageDescriptor("package2.Msg2"));
		Assert.assertNotNull(schema1.getMessageDescriptor("package3.Msg1"));
		
		// Trying to add duplicate name (fully qualified) should throw exception
		IllegalArgumentException ex = null;
		try {
			schemaBuilder1.addSchema(schemaBuilder3.build());
			schema1 = schemaBuilder1.build(); 			
		}
		catch (IllegalArgumentException e) {
			log("expected: " + e);
			ex = e;
		}
		Assert.assertNotNull(ex);
	}

	/**
	 * testSchemaSerialization - serialization, deserialization, protoc output parsing 
	 */
	@Test
	public void testSchemaSerialization() throws Exception {
		log("--- testSchemaSerialization ---");
		
		// Read protoc compiler output (deserialize)
		DynamicSchema schema1 = DynamicSchema.parseFrom(new FileInputStream("src/test/resources/PersonSchema.desc"));
		log(schema1);
		
		byte[] descBuf = schema1.toByteArray(); // serialize
		DynamicSchema schema2 = DynamicSchema.parseFrom(descBuf); // deserialize
		
		// Should be equivalent
		Assert.assertEquals(schema1.toString(), schema2.toString());
	}

	/**
	 * testSchemaDependency - nested dependencies (imports)
	 */
	@Test
	public void testSchemaDependency() throws Exception {
		log("--- testSchemaDependency ---");
		
		// Read protoc compiler output (deserialize)
		DynamicSchema schema1 = DynamicSchema.parseFrom(new FileInputStream("src/test/resources/Schema1.desc"));
		log(schema1);
		
		// schema1 should contain all imported types
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg1"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg2"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Msg3"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Person"));
		Assert.assertNotNull(schema1.getMessageDescriptor("Person.PhoneNumber"));
		Assert.assertNotNull(schema1.getEnumDescriptor("Person.PhoneType"));
	}

	/**
	 * testSchemaDependencyNoImports - missing nested dependencies (imports)
	 */
	@Test
	public void testSchemaDependencyNoImports() throws Exception {
		log("--- testSchemaDependencyNoImports ---");
		
		// Trying to parse schema descriptor with missing dependencies should throw exception
		IllegalArgumentException ex = null;
		try {
			// Read protoc compiler output (deserialize)
			DynamicSchema schema1 = DynamicSchema.parseFrom(new FileInputStream("src/test/resources/Schema1_no_imports.desc"));
			log(schema1);
		}
		catch (IllegalArgumentException e) {
			log("expected: " + e);
			ex = e;
		}
		Assert.assertNotNull(ex);
	}

	static void log(Object o) {
		System.out.println(o);
	}
}
