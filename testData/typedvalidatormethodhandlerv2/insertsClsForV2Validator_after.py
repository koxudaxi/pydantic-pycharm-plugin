from pydantic import field_validator


class Model:
    @field_validator("name")
    def validate_name(cls, <caret>):
