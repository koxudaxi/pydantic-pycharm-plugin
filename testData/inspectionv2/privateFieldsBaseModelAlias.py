from pydantic import BaseModel as BM


class Model(BM):
    _value: int


Model()
