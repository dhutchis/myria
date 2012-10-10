# Generated by the protocol buffer compiler.  DO NOT EDIT!

from google.protobuf import descriptor
from google.protobuf import message
from google.protobuf import reflection
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)



DESCRIPTOR = descriptor.FileDescriptor(
  name='column.proto',
  package='',
  serialized_pb='\n\x0c\x63olumn.proto\"\x96\x01\n\x0b\x44\x61taMessage\x12*\n\x04type\x18\x01 \x02(\x0e\x32\x1c.DataMessage.DataMessageType\x12\x12\n\noperatorID\x18\x02 \x02(\x03\x12\x1f\n\x07\x63olumns\x18\x03 \x03(\x0b\x32\x0e.ColumnMessage\"&\n\x0f\x44\x61taMessageType\x12\x07\n\x03\x45OS\x10\x00\x12\n\n\x06NORMAL\x10\x01\"\xaf\x03\n\rColumnMessage\x12.\n\x04type\x18\x01 \x02(\x0e\x32 .ColumnMessage.ColumnMessageType\x12\x12\n\nnum_tuples\x18\x02 \x02(\r\x12%\n\nint_column\x18\x03 \x01(\x0b\x32\x11.IntColumnMessage\x12\'\n\x0blong_column\x18\x04 \x01(\x0b\x32\x12.LongColumnMessage\x12)\n\x0c\x66loat_column\x18\x05 \x01(\x0b\x32\x13.FloatColumnMessage\x12+\n\rdouble_column\x18\x06 \x01(\x0b\x32\x14.DoubleColumnMessage\x12+\n\rstring_column\x18\x07 \x01(\x0b\x32\x14.StringColumnMessage\x12-\n\x0e\x62oolean_column\x18\x08 \x01(\x0b\x32\x15.BooleanColumnMessage\"V\n\x11\x43olumnMessageType\x12\x07\n\x03INT\x10\x00\x12\x08\n\x04LONG\x10\x01\x12\t\n\x05\x46LOAT\x10\x02\x12\n\n\x06\x44OUBLE\x10\x03\x12\n\n\x06STRING\x10\x04\x12\x0b\n\x07\x42OOLEAN\x10\x05\" \n\x10IntColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\"!\n\x11LongColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\"\"\n\x12\x46loatColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\"#\n\x13\x44oubleColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\"O\n\x13StringColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\x12\x15\n\rstart_indices\x18\x02 \x02(\x0c\x12\x13\n\x0b\x65nd_indices\x18\x03 \x02(\x0c\"$\n\x14\x42ooleanColumnMessage\x12\x0c\n\x04\x64\x61ta\x18\x01 \x02(\x0c\x42\x31\n$edu.washington.escience.myriad.protoB\tDataProto')



_DATAMESSAGE_DATAMESSAGETYPE = descriptor.EnumDescriptor(
  name='DataMessageType',
  full_name='DataMessage.DataMessageType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    descriptor.EnumValueDescriptor(
      name='EOS', index=0, number=0,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='NORMAL', index=1, number=1,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=129,
  serialized_end=167,
)

_COLUMNMESSAGE_COLUMNMESSAGETYPE = descriptor.EnumDescriptor(
  name='ColumnMessageType',
  full_name='ColumnMessage.ColumnMessageType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    descriptor.EnumValueDescriptor(
      name='INT', index=0, number=0,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='LONG', index=1, number=1,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='FLOAT', index=2, number=2,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='DOUBLE', index=3, number=3,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='STRING', index=4, number=4,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='BOOLEAN', index=5, number=5,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=515,
  serialized_end=601,
)


_DATAMESSAGE = descriptor.Descriptor(
  name='DataMessage',
  full_name='DataMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='type', full_name='DataMessage.type', index=0,
      number=1, type=14, cpp_type=8, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='operatorID', full_name='DataMessage.operatorID', index=1,
      number=2, type=3, cpp_type=2, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='columns', full_name='DataMessage.columns', index=2,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _DATAMESSAGE_DATAMESSAGETYPE,
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=17,
  serialized_end=167,
)


_COLUMNMESSAGE = descriptor.Descriptor(
  name='ColumnMessage',
  full_name='ColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='type', full_name='ColumnMessage.type', index=0,
      number=1, type=14, cpp_type=8, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='num_tuples', full_name='ColumnMessage.num_tuples', index=1,
      number=2, type=13, cpp_type=3, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='int_column', full_name='ColumnMessage.int_column', index=2,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='long_column', full_name='ColumnMessage.long_column', index=3,
      number=4, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='float_column', full_name='ColumnMessage.float_column', index=4,
      number=5, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='double_column', full_name='ColumnMessage.double_column', index=5,
      number=6, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='string_column', full_name='ColumnMessage.string_column', index=6,
      number=7, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='boolean_column', full_name='ColumnMessage.boolean_column', index=7,
      number=8, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _COLUMNMESSAGE_COLUMNMESSAGETYPE,
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=170,
  serialized_end=601,
)


_INTCOLUMNMESSAGE = descriptor.Descriptor(
  name='IntColumnMessage',
  full_name='IntColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='IntColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=603,
  serialized_end=635,
)


_LONGCOLUMNMESSAGE = descriptor.Descriptor(
  name='LongColumnMessage',
  full_name='LongColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='LongColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=637,
  serialized_end=670,
)


_FLOATCOLUMNMESSAGE = descriptor.Descriptor(
  name='FloatColumnMessage',
  full_name='FloatColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='FloatColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=672,
  serialized_end=706,
)


_DOUBLECOLUMNMESSAGE = descriptor.Descriptor(
  name='DoubleColumnMessage',
  full_name='DoubleColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='DoubleColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=708,
  serialized_end=743,
)


_STRINGCOLUMNMESSAGE = descriptor.Descriptor(
  name='StringColumnMessage',
  full_name='StringColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='StringColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='start_indices', full_name='StringColumnMessage.start_indices', index=1,
      number=2, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='end_indices', full_name='StringColumnMessage.end_indices', index=2,
      number=3, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=745,
  serialized_end=824,
)


_BOOLEANCOLUMNMESSAGE = descriptor.Descriptor(
  name='BooleanColumnMessage',
  full_name='BooleanColumnMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='data', full_name='BooleanColumnMessage.data', index=0,
      number=1, type=12, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=826,
  serialized_end=862,
)

_DATAMESSAGE.fields_by_name['type'].enum_type = _DATAMESSAGE_DATAMESSAGETYPE
_DATAMESSAGE.fields_by_name['columns'].message_type = _COLUMNMESSAGE
_DATAMESSAGE_DATAMESSAGETYPE.containing_type = _DATAMESSAGE;
_COLUMNMESSAGE.fields_by_name['type'].enum_type = _COLUMNMESSAGE_COLUMNMESSAGETYPE
_COLUMNMESSAGE.fields_by_name['int_column'].message_type = _INTCOLUMNMESSAGE
_COLUMNMESSAGE.fields_by_name['long_column'].message_type = _LONGCOLUMNMESSAGE
_COLUMNMESSAGE.fields_by_name['float_column'].message_type = _FLOATCOLUMNMESSAGE
_COLUMNMESSAGE.fields_by_name['double_column'].message_type = _DOUBLECOLUMNMESSAGE
_COLUMNMESSAGE.fields_by_name['string_column'].message_type = _STRINGCOLUMNMESSAGE
_COLUMNMESSAGE.fields_by_name['boolean_column'].message_type = _BOOLEANCOLUMNMESSAGE
_COLUMNMESSAGE_COLUMNMESSAGETYPE.containing_type = _COLUMNMESSAGE;
DESCRIPTOR.message_types_by_name['DataMessage'] = _DATAMESSAGE
DESCRIPTOR.message_types_by_name['ColumnMessage'] = _COLUMNMESSAGE
DESCRIPTOR.message_types_by_name['IntColumnMessage'] = _INTCOLUMNMESSAGE
DESCRIPTOR.message_types_by_name['LongColumnMessage'] = _LONGCOLUMNMESSAGE
DESCRIPTOR.message_types_by_name['FloatColumnMessage'] = _FLOATCOLUMNMESSAGE
DESCRIPTOR.message_types_by_name['DoubleColumnMessage'] = _DOUBLECOLUMNMESSAGE
DESCRIPTOR.message_types_by_name['StringColumnMessage'] = _STRINGCOLUMNMESSAGE
DESCRIPTOR.message_types_by_name['BooleanColumnMessage'] = _BOOLEANCOLUMNMESSAGE

class DataMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _DATAMESSAGE
  
  # @@protoc_insertion_point(class_scope:DataMessage)

class ColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _COLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:ColumnMessage)

class IntColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _INTCOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:IntColumnMessage)

class LongColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _LONGCOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:LongColumnMessage)

class FloatColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _FLOATCOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:FloatColumnMessage)

class DoubleColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _DOUBLECOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:DoubleColumnMessage)

class StringColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _STRINGCOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:StringColumnMessage)

class BooleanColumnMessage(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _BOOLEANCOLUMNMESSAGE
  
  # @@protoc_insertion_point(class_scope:BooleanColumnMessage)

# @@protoc_insertion_point(module_scope)