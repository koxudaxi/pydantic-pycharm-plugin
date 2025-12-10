from typing import TypedDict


class _ConfigDict(TypedDict, total=False):
    populate_by_name: bool
    from_attributes: bool
    extra: str
    frozen: bool
    validate_by_alias: bool
    validate_by_name: bool


def ConfigDict(
    populate_by_name: bool = False,
    from_attributes: bool = False,
    extra: str = None,
    frozen: bool = False,
    validate_by_alias: bool = True,
    validate_by_name: bool = False,
) -> _ConfigDict:
    return _ConfigDict(
        populate_by_name=populate_by_name,
        from_attributes=from_attributes,
        extra=extra,
        frozen=frozen,
        validate_by_alias=validate_by_alias,
        validate_by_name=validate_by_name,
    )


class BaseConfig:
    pass


class Extra:
    allow = 'allow'
    ignore = 'ignore'
    forbid = 'forbid'
