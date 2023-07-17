from warnings import warn

from ..warnings import PydanticDeprecatedSince20

DeprecationWarning = PydanticDeprecatedSince20


def validator(
        __field: str,
        *fields: str,
        pre: bool = False,
        each_item: bool = False,
        always: bool = False,
        check_fields: bool | None = None,
        allow_reuse: bool = False,
) -> Callable[[_V1ValidatorType], _V1ValidatorType]:

    warn(
        'Pydantic V1 style `@validator` validators are deprecated.'
        ' You should migrate to Pydantic V2 style `@field_validator` validators,'
        ' see the migration guide for more details',
        DeprecationWarning,
        stacklevel=2,
    )


def root_validator(
        *__args,
        pre: bool = False,
        skip_on_failure: bool = False,
        allow_reuse: bool = False,
) -> Any:
    warn(
        'Pydantic V1 style `@root_validator` validators are deprecated.'
        ' You should migrate to Pydantic V2 style `@model_validator` validators,'
        ' see the migration guide for more details',
        DeprecationWarning,
        stacklevel=2,
    )
