
def field_validator(
        __field: str,
        *fields: str,
        mode: FieldValidatorModes = 'after',
        check_fields: bool | None = None,
) -> Callable[[Any], Any]:
    pass


def model_validator(
        *,
        mode: Literal['wrap', 'before', 'after'],
) -> Any:
    pass