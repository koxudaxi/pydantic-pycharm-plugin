import pydantic


class Model(pydantic.BaseModel):
    value: str = ""


def create_model():
    return Model


factory = create_model()
factory()
