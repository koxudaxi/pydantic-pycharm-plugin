from typing import *

from enum import Enum


class BaseModel:
    class Config:
        pass

    ___slots__ = ()

    @classmethod
    def from_orm(cls, obj):
        pass

    def dict(self, include=None, exclude=None, by_alias=False, skip_defaults=None, exclude_unset=False, exclude_defaults=False, exclude_none=False):
        pass

    def json(self, include=None, exclude=None, by_alias=False, skip_defaults=None, exclude_unset=False, exclude_defaults=False, exclude_none=False, encoder=None, models_as_dict=True, **dumps_kwargs):
        pass

    def copy(self, include=None, exclude=None, update=None, deep=False):
        pass


class Extra(str, Enum):
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
        model_name: str,
        *,
        __config__: Type[BaseConfig] = None,
        __base__: Type[BaseModel] = None,
        __module__: Optional[str] = None,
        __validators__: Dict[str, classmethod] = None,
        **field_definitions: Any,
) -> Type[BaseModel]:
    pass