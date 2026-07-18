from pydantic import validator


class Model:
    @validator("name")
    def validate_name<caret>
