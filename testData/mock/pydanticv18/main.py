from typing import *

class BaseModel:
    class Config:
        pass

    ___slots__ = ()

    @classmethod
    def from_orm(cls, obj):
        pass

Model = TypeVar('Model', bound='BaseModel')


class Extra(str):
    allow = 'allow'
    ignore = 'ignore'
    forbid = 'forbid'

class GetterDict:
    pass


class json:
    def loads(self):
        pass

    def dumps(self):
        pass


class BaseConfig:
    title = None
    anystr_strip_whitespace = False
    min_anystr_length = None
    max_anystr_length = None
    validate_all = False
    extra = Extra.ignore
    allow_mutation = True
    frozen = False
    allow_population_by_field_name = False
    use_enum_values = False
    fields = {}
    validate_assignment = False
    error_msg_templates = {}
    arbitrary_types_allowed = False
    orm_mode: bool = False
    getter_dict = GetterDict
    alias_generator = None
    keep_untouched = ()
    schema_extra = {}
    json_loads = json.loads
    json_dumps = json.dumps
    json_encoders = {}

def create_model(
    __model_name: str,
    *,
    __config__: Type[BaseConfig] = None,
    __base__: Type['Model'] = None,
    __module__: str = __name__,
    __validators__: Dict[str, classmethod] = None,
    **field_definitions: Any,
) -> Type['Model']:
    pass